package dev.huynh.nfc_manager_android.models

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals

class ResponseApduTest {
    @Test
    fun `each ResponseApdu status word serializes to its two bytes`() {
        assertContentEquals(
            byteArrayOf(0x6A, 0x81.toByte()),
            ResponseApdu.FUNCTION_NOT_SUPPORTED.toByteArray(),
        )
        assertContentEquals(
            byteArrayOf(0x69.toByte(), 0x82.toByte()),
            ResponseApdu.HCE_NOT_READY.toByteArray(),
        )
        assertContentEquals(
            byteArrayOf(0x6A.toByte(), 0x82.toByte()),
            ResponseApdu.INVALID_AID.toByteArray(),
        )
        assertContentEquals(
            byteArrayOf(0x6A, 0x00.toByte()),
            ResponseApdu.NULL_COMMAND.toByteArray(),
        )
        assertContentEquals(
            byteArrayOf(0x90.toByte(), 0x00.toByte()),
            ResponseApdu.OK.toByteArray(),
        )
        assertContentEquals(
            byteArrayOf(0x67.toByte(), 0x00.toByte()),
            ResponseApdu.WRONG_LENGTH.toByteArray(),
        )
        assertContentEquals(
            byteArrayOf(0x63.toByte(), 0x00.toByte()),
            ResponseApdu.WRONG_PIN.toByteArray(),
        )
    }

    @Test
    fun `toByteArray returns a copy that callers cannot use to change the status word`() {
        ResponseApdu.OK.toByteArray().fill(0)

        assertContentEquals(byteArrayOf(0x90.toByte(), 0x00), ResponseApdu.OK.toByteArray())
    }
}
