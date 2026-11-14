package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.NfcException
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler

/**
 * Holds the active NFC Host Card Emulation (HCE) session.
 *
 * Android creates the [HostCardEmulationService] and the plugin creates the
 * [HostCardEmulationStreamHandler], so they share the session through this object.
 */
internal object HostCardEmulationConfig {
    /**
     * An active HCE session.
     *
     * @property owner The stream handler that started the session. Only it can clear the session.
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
        /**
         * Wrong PINs in a row. Only the main thread, which runs the service, reads and writes it.
         */
        var failedPinAttempts = 0

        /**
         * Sends [status] to the Flutter side.
         */
        fun emit(status: HostCardEmulationStatus) = sink.success(status.name)
    }

    /**
     * The active session, or null when HCE is not configured.
     */
    @Volatile
    var session: Session? = null
        private set

    /**
     * Replaces the active session with [session].
     */
    @Synchronized
    fun start(session: Session) {
        this.session = session
    }

    /**
     * Clears the active session if [owner] started it.
     */
    @Synchronized
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
        val wasActive =
            synchronized(this) {
                (this.session === session).also { if (it) this.session = null }
            }
        if (!wasActive) return
        session.sink.error(exception)
        session.sink.endOfStream()
    }
}
