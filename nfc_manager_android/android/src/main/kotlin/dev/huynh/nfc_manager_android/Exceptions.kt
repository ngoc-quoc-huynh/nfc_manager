package dev.huynh.nfc_manager_android

/**
 * An error reported to Flutter with a stable [code] and a fixed message.
 *
 * Messages never include received values such as commands, AIDs or PINs.
 */
internal sealed class NfcException(
    val code: String,
    override val message: String,
) : Exception(message)

/** The arguments from Flutter are missing, mistyped, or out of range. */
internal class InvalidArgumentsException :
    NfcException(
        "INVALID_ARGUMENTS",
        "The arguments are missing, mistyped, or out of range.",
    )

/** The HCE AID or PIN is missing or has the wrong length. */
internal class InvalidHceArgumentsException :
    NfcException(
        "INVALID_ARGUMENTS",
        "HCE requires an AID of 5–16 bytes and a PIN of 1–255 bytes.",
    )

/** The APDU command is malformed or longer than Android's transceive limit. */
internal class InvalidApduCommandException :
    NfcException(
        "INVALID_APDU_COMMAND",
        "The APDU command is malformed or longer than Android's transceive limit.",
    )

/** Card emulation ended after 3 wrong PINs in a row. */
internal class PinBlockedException :
    NfcException(
        "PIN_BLOCKED",
        "Card emulation ended after too many wrong PINs.",
    )

/** The NFC tag does not support ISO-DEP. */
internal class IsoDepNotSupportedException :
    NfcException(
        "ISO_DEP_NOT_SUPPORTED",
        "The NFC tag does not support ISO-DEP.",
    )

/** The device has no NFC adapter. */
internal class NfcNotSupportedException :
    NfcException(
        "NFC_NOT_SUPPORTED",
        "This device does not support NFC.",
    )

/** No tag is connected, the connection failed, or the tag was lost. */
internal class TagConnectionException :
    NfcException(
        "TAG_CONNECTION_FAILED",
        "Failed to establish a connection with the NFC tag or the tag was lost.",
    )

/** The NFC reader failed unexpectedly, or discovery started without an attached activity. */
internal class UnexpectedNfcException :
    NfcException(
        "NFC_ERROR",
        "Unexpected NFC reader error.",
    )

/**
 * The Lc byte of an APDU command is `00` or does not match its data length, or SELECT or VERIFY
 * has no data.
 *
 * Internal only: callers translate it, so it never reaches Flutter.
 */
internal class InvalidLcDataLengthException : Exception()
