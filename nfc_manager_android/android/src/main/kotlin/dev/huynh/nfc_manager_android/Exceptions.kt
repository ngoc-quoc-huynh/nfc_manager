package dev.huynh.nfc_manager_android

sealed class NfcException(
    val code: String,
    message: String,
) : Exception(message)

class InvalidApduCommandException :
    NfcException(
        "INVALID_APDU_COMMAND",
        "APDU command must be at least 4 bytes long.",
    )

class InvalidLcDataLengthException :
    NfcException(
        "INVALID_LC_DATA_LENGTH",
        "The LC value does not match the length of the Data field in the APDU command.",
    )

class InvalidHexStringLengthException :
    NfcException(
        "INVALID_HEX_STRING_LENGTH",
        "Hex string must have an even length.",
    )

class IsoDepNotSupportedException :
    NfcException(
        "ISO_DEP_NOT_SUPPORTED",
        "The NFC tag does not support  ISO-DEP.",
    )

class NfcNotSupportedException :
    NfcException(
        "NFC_NOT_SUPPORTED",
        "This device does not support NFC.",
    )

class TagLostException :
    NfcException(
        "TAG_LOST",
        "The NFC tag was lost or is no longer in range.",
    )
