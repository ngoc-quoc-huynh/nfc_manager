package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.InvalidArgumentsException
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler

/**
 * Starts and stops the HCE session for the plugin's HCE event channel.
 *
 * Listening starts a session that this handler owns; cancelling clears it. The
 * [HostCardEmulationService] reads the session to answer APDUs.
 */
internal class HostCardEmulationStreamHandler : StreamHandler {
    /**
     * Starts an HCE session from the `aid` and `pin` byte arrays in [arguments].
     *
     * Malformed arguments are reported as [InvalidArgumentsException], end the stream, and start
     * no session; a session that is already active stays in place.
     */
    override fun onListen(
        arguments: Any?,
        events: EventSink,
    ) {
        val map = arguments as? Map<*, *>
        val aid = map?.get("aid") as? ByteArray
        val pin = map?.get("pin") as? ByteArray
        if (aid == null || aid.size !in 5..16 || pin == null || pin.size !in 1..255) {
            events.error(InvalidArgumentsException())
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

    /**
     * Clears the HCE session if this handler started it.
     */
    override fun onCancel(arguments: Any?) = HostCardEmulationConfig.clear(owner = this)
}
