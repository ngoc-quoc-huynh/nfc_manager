package dev.huynh.nfc_manager_android.host_card_emulation

import io.flutter.plugin.common.EventChannel.EventSink

/**
 * Manages the configuration for NFC Host Card Emulation (HCE).
 * 
 * This class holds the necessary information for setting up and managing HCE operations,
 * including the Application Identifier (AID), PIN, and event sink for communication with Flutter.
 */
object HostCardEmulationConfig {
    /**
     * The Application Identifier (AID) for the HCE.
     */
    var aid: ByteArray? = null
        private set

    /**
     * The PIN for the HCE.
     */
    var pin: ByteArray? = null
        private set

    /**
     * The event sink for communication with Flutter.
     */
    var eventSink: EventSink? = null
        private set

    /**
     * Checks if the configuration is complete.
     */
    val isConfigured: Boolean
        get() = listOf(aid, pin, eventSink).all { it != null }

    /**
     * Configures the HCE with the given AID, PIN, and event sink.
     * This method sets the internal state of the HCE configuration to the provided values.
     * 
     * @param aid The Application Identifier which uniquely identifies the application.
     * @param pin The Personal Identification Number used for authentication.
     * @param eventSink The EventSink from Flutter used for sending events back to the Flutter side.
     */
    fun configure(
        aid: ByteArray,
        pin: ByteArray,
        eventSink: EventSink,
    ) {
        this.aid = aid
        this.pin = pin
        this.eventSink = eventSink
    }

    /**
     * Clears the HCE configuration by setting all internal state variables to null.
     */
    fun clear() {
        aid = null
        pin = null
        eventSink = null
    }
}
