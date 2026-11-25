package dev.huynh.nfc_manager_android.reader

import dev.huynh.nfc_manager_android.UnexpectedNfcException
import dev.huynh.nfc_manager_android.endWithError
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler

/** Routes the discovery channel to the current [TagReader], or reports `NFC_ERROR` without an activity. */
internal class DiscoveryStreamHandler(
    private val currentReader: () -> TagReader?,
) : StreamHandler {
    override fun onListen(
        arguments: Any?,
        events: EventSink,
    ) = currentReader()?.onListen(arguments, events) ?: events.endWithError(UnexpectedNfcException())

    override fun onCancel(arguments: Any?) {
        currentReader()?.onCancel(arguments)
    }
}
