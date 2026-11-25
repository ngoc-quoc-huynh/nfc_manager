package dev.huynh.nfc_manager_android

import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodChannel.Result

/** Replies to the method call with [exception]. */
internal fun Result.error(exception: NfcException) = error(exception.code, exception.message, null)

/** Sends [exception] as a stream error. */
internal fun EventSink.error(exception: NfcException) = error(exception.code, exception.message, null)

/** Sends [exception] as a stream error, then ends the stream. */
internal fun EventSink.endWithError(exception: NfcException) {
    error(exception)
    endOfStream()
}
