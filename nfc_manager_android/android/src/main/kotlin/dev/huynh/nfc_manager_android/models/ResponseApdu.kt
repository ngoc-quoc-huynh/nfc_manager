package dev.huynh.nfc_manager_android.models

/**
 * Represents the possible responses from an APDU command.
 */
enum class ResponseApdu(
    /**
     * The status word of the APDU response.
     */
    private val statusWord: ByteArray,
) {
    /**
     * Indicates that the function is not supported.
     */
    FUNCTION_NOT_SUPPORTED(byteArrayOf(0x6A, 0x81.toByte())),

    /**
     * Indicates that the HCE is not ready.
     */
    HCE_NOT_READY(byteArrayOf(0x69, 0x82.toByte())),

    /**
     * Indicates that the AID is invalid.
     */
    INVALID_AID(byteArrayOf(0x6A, 0x82.toByte())),

    /**
     * Indicates that the command is null.
     */
    NULL_COMMAND(byteArrayOf(0x6A, 0x00)),

    /**
     * Indicates that the command is OK.
     */
    OK(byteArrayOf(0x90.toByte(), 0x00)),

    /**
     * Indicates that the length of the command data is invalid.
     */
    WRONG_LC_LENGTH(byteArrayOf(0x67, 0x00)),

    /**
     * Indicates that the length of the response is invalid.
     */
    WRONG_LENGTH(byteArrayOf(0x6C, 0x00)),

    /**
     * Indicates that the PIN is incorrect.
     */
    WRONG_PIN(byteArrayOf(0x63, 0x00)),
    ;

    /**
     * Returns the status word of the APDU response.
     */
    operator fun invoke(): ByteArray = statusWord
}
