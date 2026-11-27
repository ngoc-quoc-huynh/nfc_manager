package dev.huynh.nfc_manager_android

import io.flutter.plugin.common.EventChannel.EventSink

/** Converts this exception to the Pigeon error Flutter receives as a PlatformException, without details. */
internal fun NfcException.toFlutterError() = FlutterError(code, message)

/** Sends [exception] as a stream error. */
internal fun EventSink.error(exception: NfcException) = error(exception.code, exception.message, null)

/** Sends [exception] as a stream error, then ends the stream. */
internal fun EventSink.endWithError(exception: NfcException) {
    error(exception)
    endOfStream()
}
