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
     * Indicates that the length of the command data is invalid.
     */
    INVALID_LC_LENGTH,

    /**
     * Indicates that the AID (Application Identifier) has been selected.
     */
    AID_SELECTED,

    /**
     * Indicates that the AID is invalid.
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
