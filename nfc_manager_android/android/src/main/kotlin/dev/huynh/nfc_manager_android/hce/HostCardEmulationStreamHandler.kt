package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.InvalidHceArgumentsException
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler

/**
 * Starts and stops the HCE session for the plugin's HCE event channel.
 *
 * Listening starts a session that this handler owns; cancelling clears it. The
 * [HostCardEmulationService] reads the session to answer APDUs. Flutter invokes the stream
 * callbacks on the platform thread, where session changes and sink callbacks must stay.
 */
internal class HostCardEmulationStreamHandler : StreamHandler {
    private var closed = false

    /**
     * Starts an HCE session from the `aid` and `pin` byte arrays in [arguments].
     *
     * An AID must be 5–16 bytes and a PIN must be 1–255 bytes. Invalid arguments report
     * [InvalidHceArgumentsException] and end the stream without creating a session.
     * A session owned by another handler stays in place. Flutter invokes this callback on the
     * platform thread.
     */
    override fun onListen(
        arguments: Any?,
        events: EventSink,
    ) {
        check(!closed) { "The HCE stream handler is detached from the engine." }
        val map = arguments as? Map<*, *>
        val aid = map?.get("aid") as? ByteArray
        val pin = map?.get("pin") as? ByteArray
        if (aid == null || aid.size !in 5..16 || pin == null || pin.size !in 1..255) {
            events.error(InvalidHceArgumentsException())
            events.endOfStream()
            return
        }
        val session =
            HostCardEmulationConfig.Session(
                owner = this,
                aid = aid,
                pin = pin,
                sink = events,
            )
        HostCardEmulationConfig.start(session)
        session.emit(HostCardEmulationStatus.READY)
    }

    /** Clears the HCE session if this handler started it, on Flutter's platform thread. */
    override fun onCancel(arguments: Any?) = clearSession()

    fun close() {
        closed = true
        clearSession()
    }

    private fun clearSession() = HostCardEmulationConfig.clear(owner = this)
}
