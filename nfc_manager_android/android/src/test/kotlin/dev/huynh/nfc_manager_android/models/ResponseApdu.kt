package dev.huynh.nfc_manager_android.models

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals

class ResponseApduTest {
    @Test
    fun `toByteArray returns correctly`() {
        assertContentEquals(
            byteArrayOf(0x6A, 0x81.toByte()),
            ResponseApdu.FUNCTION_NOT_SUPPORTED(),
        )
        assertContentEquals(
            byteArrayOf(0x69.toByte(), 0x82.toByte()),
            ResponseApdu.HCE_NOT_READY(),
        )
        assertContentEquals(
            byteArrayOf(0x6A.toByte(), 0x82.toByte()),
            ResponseApdu.INVALID_AID(),
        )
        assertContentEquals(
            byteArrayOf(0x6A, 0x00.toByte()),
            ResponseApdu.NULL_COMMAND(),
        )
        assertContentEquals(
            byteArrayOf(0x90.toByte(), 0x00.toByte()),
            ResponseApdu.OK(),
        )
        assertContentEquals(
            byteArrayOf(0x67.toByte(), 0x00.toByte()),
            ResponseApdu.WRONG_LC_LENGTH(),
        )
        assertContentEquals(
            byteArrayOf(0x6C, 0x00.toByte()),
            ResponseApdu.WRONG_LENGTH(),
        )
        assertContentEquals(
            byteArrayOf(0x63.toByte(), 0x00.toByte()),
            ResponseApdu.WRONG_PIN(),
        )
    }
}
