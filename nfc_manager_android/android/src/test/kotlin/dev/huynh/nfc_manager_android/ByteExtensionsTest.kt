package dev.huynh.nfc_manager_android

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ByteExtensionsTest {
    @Test
    fun `toHex writes two uppercase digits per byte`() =
        listOf(
            byteArrayOf() to "",
            byteArrayOf(0x1A) to "1A",
            byteArrayOf(0x0A, 0x0F) to "0A0F",
            byteArrayOf(-128, -1, -64, 0x00, 0x3C) to "80FFC0003C",
        ).forEach { (bytes, hex) -> assertEquals(hex, bytes.toHex()) }

    @Test
    fun `toUnsignedInt maps each byte to 0 to 255`() =
        listOf<Pair<Byte, Int>>(
            0.toByte() to 0,
            Byte.MAX_VALUE to 127,
            Byte.MIN_VALUE to 128,
            (-1).toByte() to 255,
        ).forEach { (byte, value) -> assertEquals(value, byte.toUnsignedInt()) }
}
