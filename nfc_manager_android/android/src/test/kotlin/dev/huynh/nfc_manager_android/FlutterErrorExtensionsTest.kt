package dev.huynh.nfc_manager_android

import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodChannel.Result
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class FlutterErrorExtensionsTest {
    @Test
    fun `Result error replies with the exception code and message`() {
        val result = mock<Result>()

        result.error(NfcNotSupportedException())

        verify(result).error("NFC_NOT_SUPPORTED", "This device does not support NFC.", null)
    }

    @Test
    fun `EventSink error sends the exception code and message`() {
        val sink = mock<EventSink>()

        sink.error(NfcNotSupportedException())

        verify(sink).error("NFC_NOT_SUPPORTED", "This device does not support NFC.", null)
    }
}
