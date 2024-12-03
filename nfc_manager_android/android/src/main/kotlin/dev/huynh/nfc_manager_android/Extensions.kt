package dev.huynh.nfc_manager_android

import io.flutter.plugin.common.MethodChannel.Result

fun ByteArray.toHex(): String =
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
