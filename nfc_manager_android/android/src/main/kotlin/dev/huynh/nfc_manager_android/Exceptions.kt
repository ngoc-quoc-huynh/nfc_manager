package dev.huynh.nfc_manager_android

sealed class NfcException(
    val code: String,
    message: String,
) : Exception(message)

class NfcNotSupportedException :
    NfcException(
        "NFC_NOT_SUPPORTED",
        "This device does not support NFC.",
    )
