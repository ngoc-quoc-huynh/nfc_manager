package dev.huynh.nfc_manager_android.host_card_emulation

import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler

/**
 * Holds the active NFC Host Card Emulation (HCE) session.
 *
 * Android creates the [HostCardEmulation] service and the plugin creates the
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
}
