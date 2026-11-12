package dev.huynh.nfc_manager_android.reader

import dev.huynh.nfc_manager_android.TagReader
import dev.huynh.nfc_manager_android.UnexpectedNfcException
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler

internal class DiscoveryStreamHandler(
    private val currentReader: () -> TagReader?,
) : StreamHandler {
    override fun onListen(
        arguments: Any?,
        events: EventSink,
    ) {
        val reader = currentReader()
        if (reader == null) {
            events.error(UnexpectedNfcException())
            events.endOfStream()
            return
        }
        reader.onListen(arguments, events)
    }

    override fun onCancel(arguments: Any?) {
        currentReader()?.onCancel(arguments)
    }
}
