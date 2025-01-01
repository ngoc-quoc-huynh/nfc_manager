package dev.huynh.nfc_manager_android.host_card_emulation

import io.flutter.plugin.common.EventChannel.EventSink

object HostCardEmulationConfig {
    var aid: ByteArray? = null
        private set
    var pin: ByteArray? = null
        private set
    var eventSink: EventSink? = null
        private set

    val isConfigured: Boolean
        get() = listOf(aid, pin, eventSink).all { it != null }

    fun configure(
        aid: ByteArray,
        pin: ByteArray,
        eventSink: EventSink,
    ) {
        this.aid = aid
        this.pin = pin
        this.eventSink = eventSink
    }

    fun clear() {
        aid = null
        pin = null
        eventSink = null
    }
}
