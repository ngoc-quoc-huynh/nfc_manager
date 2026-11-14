package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.NfcException
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import java.security.MessageDigest

/**
 * Holds the active NFC Host Card Emulation (HCE) session.
 *
 * Android creates the [HostCardEmulationService] and the plugin creates the
 * [HostCardEmulationStreamHandler], so they share the session through this object.
 * Both access it only on the main thread, so it needs no locking.
 */
internal object HostCardEmulationConfig {
    /**
     * An active HCE session.
     *
     * @property owner The stream handler that started the session; used to guard owner-based clears.
     * @property aid The Application Identifier which uniquely identifies the application.
     * @property pin The Personal Identification Number used for authentication.
     * @property sink The EventSink from Flutter used for sending events back to the Flutter side.
     */
    class Session(
        val owner: StreamHandler,
        val aid: ByteArray,
        val pin: ByteArray,
        val sink: EventSink,
    ) {
        enum class PinVerificationResult {
            VERIFIED,
            WRONG_PIN,
            BLOCKED,
        }

        /**
         * Wrong PINs in a row for this session. PIN verification owns all access to the count.
         */
        private var failedPinAttempts = 0

        /**
         * For an active session, verifies [candidate] and updates its consecutive wrong PIN count.
         * This is called from the HCE service's main-thread callback; callers emit events or end
         * the session after this state transition returns.
         */
        fun verifyPin(candidate: ByteArray): PinVerificationResult {
            if (MessageDigest.isEqual(candidate, pin)) {
                failedPinAttempts = 0
                return PinVerificationResult.VERIFIED
            }

            failedPinAttempts++
            return if (failedPinAttempts >= MAX_PIN_ATTEMPTS) {
                PinVerificationResult.BLOCKED
            } else {
                PinVerificationResult.WRONG_PIN
            }
        }

        /**
         * Sends [status] to the Flutter side.
         */
        fun emit(status: HostCardEmulationStatus) = sink.success(status.name)

        private companion object {
            const val MAX_PIN_ATTEMPTS = 3
        }
    }

    /**
     * The active session, or null when HCE is not configured.
     */
    var session: Session? = null
        private set

    /**
     * Replaces the active session with [session].
     */
    fun start(session: Session) {
        this.session = session
    }

    /**
     * Clears the active session if [owner] started it.
     */
    fun clear(owner: StreamHandler) {
        if (session?.owner === owner) session = null
    }

    /**
     * Clears [session] if it is still active, then reports [exception] and ends its stream.
     */
    fun end(
        session: Session,
        exception: NfcException,
    ) {
        if (this.session !== session) return
        this.session = null
        session.sink.error(exception)
        session.sink.endOfStream()
    }
}
