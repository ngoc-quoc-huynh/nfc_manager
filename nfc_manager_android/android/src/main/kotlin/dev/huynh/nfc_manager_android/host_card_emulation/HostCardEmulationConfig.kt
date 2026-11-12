package dev.huynh.nfc_manager_android.host_card_emulation

import io.flutter.plugin.common.EventChannel.EventSink

/**
 * Holds the active NFC Host Card Emulation (HCE) session.
 *
 * The Android-managed [HostCardEmulation] service and the plugin's stream handler are different
 * instances, so they share the session through this object.
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
        val owner: Any,
        val aid: ByteArray,
        val pin: ByteArray,
        val sink: EventSink,
    )

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
    fun clear(owner: Any) {
        if (session?.owner === owner) session = null
    }
}
