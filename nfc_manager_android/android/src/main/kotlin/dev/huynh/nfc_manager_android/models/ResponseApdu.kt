package dev.huynh.nfc_manager_android.models

/**
 * Represents the possible responses from an APDU command.
 */
internal enum class ResponseApdu(
    /**
     * The status word of the APDU response.
     */
    private val statusWord: ByteArray,
) {
    /**
     * 69 85: conditions of use not satisfied, sent when VERIFY arrives before
     * the configured AID is selected.
     */
    AID_NOT_SELECTED(byteArrayOf(0x69, 0x85.toByte())),

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
     * 69 83: authentication method blocked, sent when too many wrong PINs end the session.
     */
    PIN_BLOCKED(byteArrayOf(0x69, 0x83.toByte())),

    /**
     * Indicates that the command is too short or its Lc does not match its data length.
     */
    WRONG_LENGTH(byteArrayOf(0x67, 0x00)),

    /**
     * Indicates that the PIN is incorrect.
     */
    WRONG_PIN(byteArrayOf(0x63, 0x00)),
    ;

    /**
     * Returns a copy of the status word, so callers cannot change the shared constant.
     */
    fun toByteArray(): ByteArray = statusWord.copyOf()
}
