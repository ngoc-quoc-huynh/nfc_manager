package dev.huynh.nfc_manager_android.host_card_emulation

enum class HostCardEmulationStatus {
    NOT_CONFIGURED,
    READY,
    INVALID_COMMAND,
    INVALID_LC_LENGTH,
    AID_SELECTED,
    INVALID_AID,
    PIN_VERIFIED,
    WRONG_PIN,
    FUNCTION_NOT_SUPPORTED,
    TAG_DISCONNECTED,
    DEACTIVATED,
}
