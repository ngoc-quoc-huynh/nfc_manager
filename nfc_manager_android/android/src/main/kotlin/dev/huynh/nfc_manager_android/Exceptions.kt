package dev.huynh.nfc_manager_android

/**
 * Base class for all NFC exceptions.
 * 
 * @param code The error code.
 * @param message The error message.
 */
sealed class NfcException(
    val code: String,
    message: String,
) : Exception(message)

/**
 * Exception thrown when the APDU command is invalid because it must be at
 * least 4 bytes long.
 */
class InvalidApduCommandException :
    NfcException(
        "INVALID_APDU_COMMAND",
        "APDU command must be at least 4 bytes long.",
    )

/**
 * Exception thrown when the LC value does not match the length of the Data field in the APDU command.
 */
class InvalidLcDataLengthException :
    NfcException(
        "INVALID_LC_DATA_LENGTH",
        "The LC value does not match the length of the Data field in the APDU command.",
    )

/**
 * Exception thrown when the NFC tag does not support ISO-DEP.
 */
class IsoDepNotSupportedException :
    NfcException(
        "ISO_DEP_NOT_SUPPORTED",
        "The NFC tag does not support ISO-DEP.",
    )

/**
 * Exception thrown when the device does not support NFC.
 */
class NfcNotSupportedException :
    NfcException(
        "NFC_NOT_SUPPORTED",
        "This device does not support NFC.",
    )

/**
 * Exception thrown when the tag connection failed.
 */
class TagConnectionException :
    NfcException(
        "TAG_CONNECTION_FAILED",
        "Failed to establish a connection with the NFC tag or the tag was lost.",
    )
