package dev.huynh.nfc_manager_android.utils

import dev.huynh.nfc_manager_android.NfcException
import io.flutter.plugin.common.MethodChannel.Result

fun ByteArray.toHexString(): String =
    joinToString(separator = "") { byte ->
        "%02x".format(byte)
    }.uppercase()

fun Result.error(nfcException: NfcException) =
    error(
        nfcException.code,
        nfcException.message,
        null,
    )

fun <T> Result.trySuccess(action: () -> T) {
    try {
        val result = action()
        success(result)
    } catch (e: NfcException) {
        error(e)
    }
}

fun Byte.toUnsignedInt(): Int = toInt() and 0xFF
