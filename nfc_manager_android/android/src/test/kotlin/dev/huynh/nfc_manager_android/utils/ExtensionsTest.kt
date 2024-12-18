package dev.huynh.nfc_manager_android

import dev.huynh.nfc_manager_android.utils.error
import dev.huynh.nfc_manager_android.utils.toHexString
import dev.huynh.nfc_manager_android.utils.toUnsignedInt
import dev.huynh.nfc_manager_android.utils.trySuccess
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodChannel.Result
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertEquals

class ExtensionsTest {
    private val mockResult = mock<Result>()

    @Test
    fun `toHexString should return an empty string for an empty byte array`() =
        assertEquals(
            "",
            byteArrayOf().toHexString(),
        )

    @Test
    fun `toHexString should convert a single byte to its hex representation`() =
        assertEquals(
            "1A",
            byteArrayOf(0x1A).toHexString(),
        )

    @Test
    fun `toHexString should convert multiple bytes to their hex representation`() =
        assertEquals(
            "1AFF003C",
            byteArrayOf(0x1A, 0xFF.toByte(), 0x00, 0x3C).toHexString(),
        )

    @Test
    fun `toHexString should handle signed bytes correctly`() =
        assertEquals(
            "80FFC0",
            byteArrayOf(-128, -1, -64).toHexString(),
        )

    @Test
    fun `Result error should pass NfcException code and message`() {
        val nfcException = NfcNotSupportedException()
        mockResult.error(nfcException)
        verify(mockResult).error(
            "NFC_NOT_SUPPORTED",
            "This device does not support NFC.",
            null,
        )
    }

    @Test
    fun `EventSink error should pass NfcException code and message`() {
        val nfcException = NfcNotSupportedException()
        val mockEventSink = mock<EventSink>()
        mockEventSink.error(nfcException)
        verify(mockEventSink).error(
            "NFC_NOT_SUPPORTED",
            "This device does not support NFC.",
            null,
        )
    }

    @Test
    fun `trySuccess should call success when action succeeds`() {
        mockResult.trySuccess({ "Success" })
        verify(mockResult).success("Success")
    }

    @Test
    fun `trySuccess should call error when action throws`() {
        mockResult.trySuccess({ throw NfcNotSupportedException() })
        verify(mockResult).error(
            "NFC_NOT_SUPPORTED",
            "This device does not support NFC.",
            null,
        )
    }

    @Test
    fun `toUnsignedInt returns positive byte correctly`() {
        val byte: Byte = 100
        val unsignedInt = byte.toUnsignedInt()
        assertEquals(100, unsignedInt)
    }

    @Test
    fun `toUnsignedInt returns negative byte correctly`() {
        val byte: Byte = -100
        val unsignedInt = byte.toUnsignedInt()
        assertEquals(156, unsignedInt)
    }

    @Test
    fun `toUnsignedInt returns zero byte correctly`() {
        val byte: Byte = 0
        val unsignedInt = byte.toUnsignedInt()
        assertEquals(0, unsignedInt)
    }

    @Test
    fun `toUnsignedInt returns max byte correctly`() {
        val byte: Byte = Byte.MAX_VALUE
        val unsignedInt = byte.toUnsignedInt()
        assertEquals(127, unsignedInt)
    }

    @Test
    fun `toUnsignedInt returns min byte correctly`() {
        val byte: Byte = Byte.MIN_VALUE
        val unsignedInt = byte.toUnsignedInt()
        assertEquals(128, unsignedInt)
    }
}
