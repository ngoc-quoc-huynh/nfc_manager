package dev.huynh.nfc_manager_android

private const val HEX_DIGITS = "0123456789ABCDEF"

/**
 * Returns two uppercase hexadecimal digits per byte, for example `04A1B2C3`.
 *
 * Discovery sends tag IDs in this format. `toHexString(HexFormat.UpperCase)` from the stdlib
 * needs an opt-in before Kotlin 2.2, and the plugin compiles with the host app's Kotlin version.
 */
internal fun ByteArray.toHex(): String {
    val chars = CharArray(size * 2)
    forEachIndexed { index, byte ->
        val value = byte.toUnsignedInt()
        chars[index * 2] = HEX_DIGITS[value shr 4]
        chars[index * 2 + 1] = HEX_DIGITS[value and 0x0F]
    }
    return String(chars)
}

/** Returns the byte as an unsigned value in 0–255. */
internal fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
