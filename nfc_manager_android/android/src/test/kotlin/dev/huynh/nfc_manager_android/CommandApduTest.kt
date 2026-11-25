package dev.huynh.nfc_manager_android

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CommandApduTest {
    private val header = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    private val maxData = ByteArray(255) { it.toByte() }

    @Test
    fun `fromByteArray rejects commands shorter than 4 bytes`() =
        (0..3).forEach { size ->
            assertThrows<InvalidApduCommandException> { CommandApdu.fromByteArray(ByteArray(size)) }
        }

    @Test
    fun `fromByteArray rejects an Lc that is 00 or does not match the data`() =
        listOf(
            // Lc 00 starts an extended-length encoding.
            header + byteArrayOf(0x00, 0x00),
            // Lc says 2 data bytes, but only 1 follows.
            header + byteArrayOf(0x02, 0x0A),
            // Lc says 1 data byte; one byte follows data and Le.
            header + byteArrayOf(0x01, 0x0A, 0x00, 0x00),
        ).forEach { command ->
            assertThrows<InvalidLcDataLengthException> { CommandApdu.fromByteArray(command) }
        }

    @Test
    fun `fromByteArray parses the header, data and Le`() =
        listOf(
            Triple(header, null, null),
            Triple(header + byteArrayOf(0x10), null, 0x10.toByte()),
            Triple(header + byteArrayOf(0x01, 0x0A), byteArrayOf(0x0A), null),
            Triple(header + byteArrayOf(0x02, 0x0A, 0x0B, 0x00), byteArrayOf(0x0A, 0x0B), 0x00.toByte()),
            Triple(header + byteArrayOf(0xFF.toByte()) + maxData + byteArrayOf(0x00), maxData, 0x00.toByte()),
        ).forEach { (bytes, data, le) ->
            val command = CommandApdu.fromByteArray(bytes)

            assertContentEquals(header, byteArrayOf(command.cla, command.ins, command.p1, command.p2))
            assertContentEquals(data, command.data)
            assertEquals(le, command.le)
        }
}
