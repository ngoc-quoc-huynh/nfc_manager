package dev.huynh.nfc_manager_android.host_card_emulation

import dev.huynh.nfc_manager_android.InvalidArgumentsException
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler

/**
 * Starts and stops the HCE session for the plugin's HCE event channel.
 *
 * Listening starts a session that this handler owns; cancelling clears it. The
 * [HostCardEmulation] service reads the session to answer APDUs.
 */
internal class HostCardEmulationStreamHandler : StreamHandler {
    /**
     * Starts an HCE session from the `aid` and `pin` byte arrays in [arguments].
     *
     * Malformed arguments are reported as [InvalidArgumentsException] and leave HCE unconfigured.
     */
    override fun onListen(
        arguments: Any?,
        events: EventSink,
    ) {
        val map = arguments as? Map<*, *>
        val aid = map?.get("aid") as? ByteArray
        val pin = map?.get("pin") as? ByteArray
        if (aid == null || pin == null) {
            events.error(InvalidArgumentsException())
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
