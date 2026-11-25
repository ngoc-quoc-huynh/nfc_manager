package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.InvalidHceArgumentsException
import dev.huynh.nfc_manager_android.endWithError
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler

/**
 * Starts and stops HCE sessions for the plugin's HCE event channel.
 *
 * Listening with an `aid` (5–16 bytes) and a `pin` (1–255 bytes) starts a session that this
 * handler owns and replaces any active session. Invalid arguments report
 * [InvalidHceArgumentsException], end the stream, and leave any active session in place.
 * Cancelling clears this handler's session. Flutter calls it on the main thread, where the
 * session changes.
 */
internal class HostCardEmulationStreamHandler : StreamHandler {
    // Flutter can still deliver a listen that was queued before the engine detached.
    private var closed = false

    override fun onListen(
        arguments: Any?,
        events: EventSink,
    ) {
        check(!closed) { "The HCE stream handler is detached from the engine." }
        val map = arguments as? Map<*, *>
        val aid = map?.get("aid") as? ByteArray
        val pin = map?.get("pin") as? ByteArray
        if (aid == null || aid.size !in AID_LENGTH || pin == null || pin.size !in PIN_LENGTH) {
            events.endWithError(InvalidHceArgumentsException())
            return
        }
        val session = HostCardEmulationSession(owner = this, aid = aid, pin = pin, sink = events)
        HostCardEmulationSessionHolder.start(session)
        session.emit(HostCardEmulationStatus.READY)
    }

    override fun onCancel(arguments: Any?) = clearSession()

    /** Clears this handler's session and rejects later listens, when the engine detaches. */
    fun close() {
        closed = true
        clearSession()
    }

    private fun clearSession() = HostCardEmulationSessionHolder.clear(owner = this)

    private companion object {
        val AID_LENGTH = 5..16
        val PIN_LENGTH = 1..255
    }
}
