package dev.huynh.nfc_manager_android.utils

import dev.huynh.nfc_manager_android.NfcException
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodChannel.Result

private const val HEX_DIGITS = "0123456789ABCDEF"

/**
 * Converts a ByteArray to an uppercase hexadecimal string without separators.
 *
 * Formats manually because the stdlib `HexFormat` needs an opt-in before Kotlin 2.2,
 * and the plugin compiles with the host app's Kotlin version.
 *
 * @return The hexadecimal string representation of the ByteArray.
 */
internal fun ByteArray.toHexString(): String {
    val chars = CharArray(size * 2)
    forEachIndexed { index, byte ->
        val value = byte.toUnsignedInt()
        chars[index * 2] = HEX_DIGITS[value shr 4]
        chars[index * 2 + 1] = HEX_DIGITS[value and 0x0F]
    }
    return String(chars)
}

/**
 * Emits an error status to the Flutter side.
 *
 * @param exception The `NfcException` to emit, which contains the error code and message to be sent.
 */
internal fun Result.error(exception: NfcException) =
    error(
        exception.code,
        exception.message,
        null,
    )

/**
 * Emits an error status to the Flutter side.
 *
 * @param exception The `NfcException` to emit, which contains the error code and message to be sent.
 */
internal fun EventSink.error(exception: NfcException) =
    error(
        exception.code,
        exception.message,
        null,
    )

/**
 * Converts a Byte to an unsigned integer.
 *
 * @return The unsigned integer representation of the Byte.
 */
internal fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
