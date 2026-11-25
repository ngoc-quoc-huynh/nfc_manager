package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.NfcException
import dev.huynh.nfc_manager_android.endWithError
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import java.security.MessageDigest

/**
 * An HCE session started from Flutter.
 *
 * @property owner The stream handler that started the session; only it can clear the session.
 */
internal class HostCardEmulationSession(
    val owner: StreamHandler,
    private val aid: ByteArray,
    private val pin: ByteArray,
    private val sink: EventSink,
) {
    enum class PinVerificationResult {
        VERIFIED,
        WRONG_PIN,
        BLOCKED,
    }

    /** Wrong PINs in a row. */
    private var failedPinAttempts = 0

    /** PIN attempts left. A wrong PIN on the last attempt ends the session. */
    val pinAttemptsLeft: Int get() = MAX_PIN_ATTEMPTS - failedPinAttempts

    /** Whether [candidate] is the session's AID. */
    fun matchesAid(candidate: ByteArray): Boolean = candidate.contentEquals(aid)

    /** Checks [candidate] in constant time and updates the wrong PIN count. */
    fun verifyPin(candidate: ByteArray): PinVerificationResult {
        if (MessageDigest.isEqual(candidate, pin)) {
            failedPinAttempts = 0
            return PinVerificationResult.VERIFIED
        }

        failedPinAttempts++
        return if (pinAttemptsLeft == 0) PinVerificationResult.BLOCKED else PinVerificationResult.WRONG_PIN
    }

    /** Sends [status] to Flutter. */
    fun emit(status: HostCardEmulationStatus) = sink.success(status.name)

    /** Reports [exception] to Flutter and ends the stream. */
    fun fail(exception: NfcException) = sink.endWithError(exception)

    private companion object {
        const val MAX_PIN_ATTEMPTS = 3
    }
}
