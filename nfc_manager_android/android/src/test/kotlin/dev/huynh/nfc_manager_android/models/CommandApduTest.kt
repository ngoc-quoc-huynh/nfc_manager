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
        assertEquals(
            CommandApdu(
                cla = 0x00.toByte(),
                ins = 0x00.toByte(),
                p1 = 0x00.toByte(),
                p2 = 0x00.toByte(),
            ),
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                ),
            ),
        )
    }

    @Test
    fun `fromByteArray returns CLA, INS, P1, P2, LE when command is exactly 5 bytes`() {
        assertEquals(
            CommandApdu(
                cla = 0x00.toByte(),
                ins = 0x00.toByte(),
                p1 = 0x00.toByte(),
                p2 = 0x00.toByte(),
                le = 0x00.toByte(),
            ),
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                ),
            ),
        )
    }

    @Test
    fun `fromByteArray returns CLA, INS, P1, P2, LC, LE when command is exactly 6 bytes and LC is 0x00`() {
        assertEquals(
            CommandApdu(
                cla = 0x00.toByte(),
                ins = 0x00.toByte(),
                p1 = 0x00.toByte(),
                p2 = 0x00.toByte(),
                lc = 0x00.toByte(),
                le = 0x00.toByte(),
            ),
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                ),
            ),
        )
    }

    @Test
    fun `fromByteArray returns CLA, INS, P1, P2, LC, DATA when command is exactly 6 bytes and LC is 0x01`() {
        assertEquals(
            CommandApdu(
                cla = 0x00.toByte(),
                ins = 0x00.toByte(),
                p1 = 0x00.toByte(),
                p2 = 0x00.toByte(),
                lc = 0x01.toByte(),
                data = byteArrayOf(0x00.toByte()),
            ),
            CommandApdu.fromByteArray(
                byteArrayOf(
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x01.toByte(),
                    0x00.toByte(),
                ),
            ),
        )
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
        assertEquals(
            CommandApdu(
                cla = 0x00.toByte(),
                ins = 0x00.toByte(),
                p1 = 0x00.toByte(),
                p2 = 0x00.toByte(),
                lc = 0x02.toByte(),
                data = byteArrayOf(0x00.toByte(), 0x00.toByte()),
            ),
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
            ),
        )
    }

    @Test
    fun `fromByteArray returns CLA, INS, P1, P2, LC, Data, LE when command is more than 6 bytes and LE is present`() {
        assertEquals(
            CommandApdu(
                cla = 0x00.toByte(),
                ins = 0x00.toByte(),
                p1 = 0x00.toByte(),
                p2 = 0x00.toByte(),
                lc = 0x01.toByte(),
                data = byteArrayOf(0x00.toByte()),
                le = 0x00.toByte(),
            ),
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
            ),
        )
    }

    @Test
    fun `toByteArray returns correct for  CLA, INS, P1, P2`() {
        assertContentEquals(
            byteArrayOf(
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            CommandApdu(
                cla = 0x00.toByte(),
                ins = 0x00.toByte(),
                p1 = 0x00.toByte(),
                p2 = 0x00.toByte(),
            ).toByteArray(),
        )
    }

    @Test
    fun `toByteArray returns correct for  CLA, INS, P1, P2, LC, DATA`() {
        assertContentEquals(
            byteArrayOf(
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x00.toByte(),
            ),
            CommandApdu(
                cla = 0x00.toByte(),
                ins = 0x00.toByte(),
                p1 = 0x00.toByte(),
                p2 = 0x00.toByte(),
                lc = 0x01.toByte(),
                data = byteArrayOf(0x00.toByte()),
            ).toByteArray(),
        )
    }

    @Test
    fun `toByteArray returns correct for  CLA, INS, P1, P2, LC, LE`() {
        assertContentEquals(
            byteArrayOf(
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            CommandApdu(
                cla = 0x00.toByte(),
                ins = 0x00.toByte(),
                p1 = 0x00.toByte(),
                p2 = 0x00.toByte(),
                lc = 0x00.toByte(),
                le = 0x00.toByte(),
            ).toByteArray(),
        )
    }

    @Test
    fun `toByteArray returns correct for  CLA, INS, P1, P2, LC, DATA, LE`() {
        assertContentEquals(
            byteArrayOf(
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ),
            CommandApdu(
                cla = 0x00.toByte(),
                ins = 0x00.toByte(),
                p1 = 0x00.toByte(),
                p2 = 0x00.toByte(),
                lc = 0x01.toByte(),
                data = byteArrayOf(0x00.toByte()),
                le = 0x00.toByte(),
            ).toByteArray(),
        )
    }
}
