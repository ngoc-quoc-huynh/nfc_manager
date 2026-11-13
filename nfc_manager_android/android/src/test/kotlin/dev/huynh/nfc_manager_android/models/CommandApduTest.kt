package dev.huynh.nfc_manager_android.models

import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.InvalidLcDataLengthException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CommandApduTest {
    @Test
    fun `fromByteArray throws InvalidApduCommandException when command is empty`() {
        assertThrows<InvalidApduCommandException> { CommandApdu.fromByteArray(byteArrayOf()) }
    }

    @Test
    fun `fromByteArray throws InvalidApduCommandException when command is smaller than 4 bytes`() {
        assertThrows<InvalidApduCommandException> { CommandApdu.fromByteArray(byteArrayOf(0x00.toByte())) }
        assertThrows<InvalidApduCommandException> {
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                ),
            )
        }
        assertThrows<InvalidApduCommandException> {
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                ),
            )
        }
    }

    @Test
    fun `fromByteArray returns CLA, INS, P1, P2 when command is exactly 4 bytes`() {
        val command = CommandApdu.fromByteArray(byteArrayOf(0x01, 0x02, 0x03, 0x04))

        assertCommand(command, header = byteArrayOf(0x01, 0x02, 0x03, 0x04))
    }

    @Test
    fun `fromByteArray returns CLA, INS, P1, P2, LE when command is exactly 5 bytes`() {
        val command =
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                ),
            )

        assertCommand(command, le = 0x00)
    }

    @Test
    fun `fromByteArray throws InvalidLcDataLengthException when command is exactly 6 bytes and LC is 0x00`() {
        assertThrows<InvalidLcDataLengthException> {
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                ),
            )
        }
    }

    @Test
    fun `fromByteArray returns CLA, INS, P1, P2, LC, DATA when command is exactly 6 bytes and LC is 0x01`() {
        val command =
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                    0x00.toByte(),
                ),
            )

        assertCommand(command, data = byteArrayOf(0x00))
    }

    @Test
    fun `fromByteArray throws InvalidLcDataLengthException when data is shorter than LC`() {
        assertThrows<InvalidLcDataLengthException> {
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x02.toByte(),
                    0x00.toByte(),
                ),
            )
        }
    }

    @Test
    fun `fromByteArray throws InvalidLcDataLengthException when data is greater than LC`() {
        assertThrows<InvalidLcDataLengthException> {
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                ),
            )
        }
    }

    @Test
    fun `fromByteArray returns CLA, INS, P1, P2, LC, Data when command is more than 6 bytes`() {
        val command =
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x02.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                ),
            )

        assertCommand(command, data = byteArrayOf(0x00, 0x00))
    }

    @Test
    fun `fromByteArray returns CLA, INS, P1, P2, LC, Data, LE when command is more than 6 bytes and LE is present`() {
        val command =
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                ),
            )

        assertCommand(command, data = byteArrayOf(0x00), le = 0x00)
    }

    /**
     * Asserts every field of [command]; [header] holds the expected CLA, INS, P1 and P2.
     */
    private fun assertCommand(
        command: CommandApdu,
        header: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00),
        data: ByteArray? = null,
        le: Byte? = null,
    ) {
        assertContentEquals(header, byteArrayOf(command.cla, command.ins, command.p1, command.p2))
        assertContentEquals(data, command.data)
        assertEquals(le, command.le)
    }
}
