package dev.huynh.nfc_manager_android.hce

/** HCE readiness and command outcomes sent to Flutter by name. */
internal enum class HostCardEmulationStatus {
    /** The HCE configuration is stored. */
    READY,

    /** The command is shorter than the 4-byte header. */
    INVALID_COMMAND,

    /**
     * The Lc byte is `00` or does not match the command data, or a SELECT/VERIFY command has no
     * data.
     */
    INVALID_LC_LENGTH,

    /** The received AID matched the configured AID. */
    AID_SELECTED,

    /** The received AID did not match the configured AID. */
    INVALID_AID,

    /** The received PIN matched the configured PIN. */
    PIN_VERIFIED,

    /**
     * The received PIN did not match. The third wrong PIN in a row ends the session with
     * [dev.huynh.nfc_manager_android.PinBlockedException] instead.
     */
    WRONG_PIN,

    /** The command is not SELECT AID (`A4 04`) or VERIFY (`20 00`). */
    FUNCTION_NOT_SUPPORTED,

    /** The NFC link ended or the reader switched to another service. */
    TAG_DISCONNECTED,
}
