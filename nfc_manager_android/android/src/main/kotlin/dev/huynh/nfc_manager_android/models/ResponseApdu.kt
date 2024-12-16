package dev.huynh.nfc_manager_android.models

enum class ResponseApdu(
    private val statusWord: ByteArray,
) {
    FUNCTION_NOT_SUPPORTED(byteArrayOf(0x6A, 0x81.toByte())),
    HCE_NOT_READY(byteArrayOf(0x69, 0x82.toByte())),
    INVALID_AID(byteArrayOf(0x6A, 0x82.toByte())),
    NULL_COMMAND(byteArrayOf(0x6A, 0x00)),
    OK(byteArrayOf(0x90.toByte(), 0x00)),
    UNKNOWN(byteArrayOf(0x67, 0x00)),
    WRONG_LENGTH(byteArrayOf(0x6C, 0x00)),
    WRONG_PIN(byteArrayOf(0x63, 0x00)),
    ;

    operator fun invoke(): ByteArray = statusWord
}
