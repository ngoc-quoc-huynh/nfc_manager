package dev.huynh.nfc_manager_android.host_card_emulation

object HostCardEmulationConfig {
    var aid: ByteArray? = null
        private set
    var pin: ByteArray? = null
        private set

    val isConfigured: Boolean
        get() = aid != null && pin != null

    fun configure(
        aid: ByteArray,
        pin: ByteArray,
    ) {
        this.aid = aid
        this.pin = pin
    }

    fun clear() {
        aid = null
        pin = null
    }
}
