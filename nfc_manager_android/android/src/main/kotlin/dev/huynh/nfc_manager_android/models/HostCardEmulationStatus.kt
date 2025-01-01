package dev.huynh.nfc_manager_android.models

enum class HostCardEmulationStatus {
    READY,
    INVALID_COMMAND,
    INVALID_LC_LENGTH,
    AID_SELECTED,
    INVALID_AID,
    PIN_VERIFIED,
    WRONG_PIN,
    FUNCTION_NOT_SUPPORTED,
    TAG_DISCONNECTED,
}
