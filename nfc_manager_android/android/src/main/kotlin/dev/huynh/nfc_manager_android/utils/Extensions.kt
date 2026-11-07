package dev.huynh.nfc_manager_android.utils

import dev.huynh.nfc_manager_android.NfcException
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodChannel.Result

/**
 * Converts a ByteArray to a hexadecimal string.
 * 
 * @return The hexadecimal string representation of the ByteArray.
 */
fun ByteArray.toHexString(): String =
    joinToString(separator = "") { byte ->
        "%02x".format(byte)
    }.uppercase()

/**
 * Emits an error status to the Flutter side.
 * 
 * @param exception The `NfcException` to emit, which contains the error code and message to be sent.
 */
fun Result.error(exception: NfcException) =
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
fun EventSink.error(exception: NfcException) =
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
fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
