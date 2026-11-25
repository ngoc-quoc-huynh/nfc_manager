package dev.huynh.nfc_manager_android.hce

/** The status word (SW1 SW2) that the HCE service answers to a reader. */
internal class ResponseApdu private constructor(
    private val sw1: Int,
    private val sw2: Int,
) {
    /** Returns a new array, so callers cannot change a shared constant. */
    fun toByteArray(): ByteArray = byteArrayOf(sw1.toByte(), sw2.toByte())

    companion object {
        /** `90 00`: success. */
        val OK = ResponseApdu(0x90, 0x00)

        /** `67 00`: the command is too short or its Lc does not match its data. */
        val WRONG_LENGTH = ResponseApdu(0x67, 0x00)

        /** `69 82`: HCE is not started. */
        val HCE_NOT_READY = ResponseApdu(0x69, 0x82)

        /** `69 83`: too many wrong PINs ended the session. */
        val PIN_BLOCKED = ResponseApdu(0x69, 0x83)

        /** `69 85`: VERIFY arrived before the configured AID was selected. */
        val AID_NOT_SELECTED = ResponseApdu(0x69, 0x85)

        /** `6A 00`: Android passed no command. */
        val NULL_COMMAND = ResponseApdu(0x6A, 0x00)

        /** `6A 81`: the command is not SELECT by name or VERIFY. */
        val FUNCTION_NOT_SUPPORTED = ResponseApdu(0x6A, 0x81)

        /** `6A 82`: the selected AID is not the configured one. */
        val INVALID_AID = ResponseApdu(0x6A, 0x82)

        /** `63 00`: wrong PIN. */
        val WRONG_PIN = ResponseApdu(0x63, 0x00)
    }
}
