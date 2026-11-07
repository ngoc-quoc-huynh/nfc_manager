package dev.huynh.nfc_manager_android.models

/**
 * Represents the status of the host card emulation (HCE) operation.
 */
enum class HostCardEmulationStatus {
    /**
     * Indicates that the HCE is ready for operations.
     */
    READY,

    /**
     * Indicates that the command is invalid because it must be at least 4 bytes
     * long.
     */
    INVALID_COMMAND,

    /**
     * The declared Lc does not match the command data length, or a SELECT/VERIFY
     * command is missing required data.
     */
    INVALID_LC_LENGTH,

    /**
     * Indicates that the AID (Application Identifier) has been selected.
     */
    AID_SELECTED,

    /**
     * The received AID did not match the configured AID.
     */
    INVALID_AID,

    /**
     * Indicates that the PIN has been verified.
     */
    PIN_VERIFIED,

    /**
     * Indicates that the PIN is incorrect.
     */
    WRONG_PIN,

    /**
     * Indicates that the function is not supported.
     */
    FUNCTION_NOT_SUPPORTED,

    /**
     * Indicates that the tag is disconnected.
     */
    TAG_DISCONNECTED,
}
