package dev.huynh.nfc_manager_android.hce

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class ResponseApduTest {
    @Test
    fun `each status word serializes to its two bytes`() =
        listOf(
            ResponseApdu.OK to byteArrayOf(0x90.toByte(), 0x00),
            ResponseApdu.WRONG_LENGTH to byteArrayOf(0x67, 0x00),
            ResponseApdu.HCE_NOT_READY to byteArrayOf(0x69, 0x82.toByte()),
            ResponseApdu.PIN_BLOCKED to byteArrayOf(0x69, 0x83.toByte()),
            ResponseApdu.AID_NOT_SELECTED to byteArrayOf(0x69, 0x85.toByte()),
            ResponseApdu.NULL_COMMAND to byteArrayOf(0x6A, 0x00),
            ResponseApdu.FUNCTION_NOT_SUPPORTED to byteArrayOf(0x6A, 0x81.toByte()),
            ResponseApdu.INVALID_AID to byteArrayOf(0x6A, 0x82.toByte()),
            ResponseApdu.INSTRUCTION_NOT_SUPPORTED to byteArrayOf(0x6D, 0x00),
        ).forEach { (response, bytes) -> assertContentEquals(bytes, response.toByteArray()) }

    @Test
    fun `wrongPin encodes the attempts left as 63 CX`() =
        listOf(15 to 0xCF, 2 to 0xC2, 1 to 0xC1).forEach { (attemptsLeft, sw2) ->
            assertContentEquals(byteArrayOf(0x63, sw2.toByte()), ResponseApdu.wrongPin(attemptsLeft).toByteArray())
        }

    @Test
    fun `wrongPin rejects attempts outside 1 to 15`() =
        listOf(0, 16).forEach { attemptsLeft ->
            assertFailsWith<IllegalArgumentException> { ResponseApdu.wrongPin(attemptsLeft) }
        }

    @Test
    fun `toByteArray returns a new array that callers cannot use to change the status word`() {
        ResponseApdu.OK.toByteArray().fill(0)

        assertContentEquals(byteArrayOf(0x90.toByte(), 0x00), ResponseApdu.OK.toByteArray())
    }
}
