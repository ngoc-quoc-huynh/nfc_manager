package dev.huynh.nfc_manager_android

import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FlutterErrorExtensionsTest {
    @Test
    fun `toFlutterError keeps the exception code and message without details`() {
        val error = NfcNotSupportedException().toFlutterError()

        assertEquals("NFC_NOT_SUPPORTED", error.code)
        assertEquals("This device does not support NFC.", error.message)
        assertNull(error.details)
    }

    @Test
    fun `EventSink error sends the exception code and message without details`() {
        val sink = mock<EventSink>()

        sink.error(NfcNotSupportedException())

        verify(sink).error("NFC_NOT_SUPPORTED", "This device does not support NFC.", null)
    }
}
