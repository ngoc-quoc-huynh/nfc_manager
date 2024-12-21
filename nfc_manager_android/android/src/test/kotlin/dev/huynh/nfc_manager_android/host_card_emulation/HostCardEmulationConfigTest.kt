package dev.huynh.nfc_manager_android.host_card_emulation

import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HostCardEmulationConfigTest {
    private val mockEventSink = mock<EventSink>()

    @BeforeEach
    fun setup() = HostCardEmulationConfig.clear()

    @Test
    fun `initial state aid and pin are null`() {
        assertNull(HostCardEmulationConfig.aid)
        assertNull(HostCardEmulationConfig.pin)
        assertNull(HostCardEmulationConfig.eventSink)
        assertFalse(HostCardEmulationConfig.isConfigured)
    }

    @Test
    fun `configure sets aid and pin`() {
        val aid = byteArrayOf()
        val pin = byteArrayOf()

        HostCardEmulationConfig.configure(
            aid = aid,
            pin = pin,
            eventSink = mockEventSink,
        )

        assertContentEquals(aid, HostCardEmulationConfig.aid)
        assertContentEquals(pin, HostCardEmulationConfig.pin)
        assertEquals(mockEventSink, HostCardEmulationConfig.eventSink)
        assertTrue(HostCardEmulationConfig.isConfigured)
    }

    @Test
    fun `clear resets aid and pin`() {
        HostCardEmulationConfig.configure(
            aid = byteArrayOf(),
            pin = byteArrayOf(),
            eventSink = mockEventSink,
        )
        HostCardEmulationConfig.clear()

        assertNull(HostCardEmulationConfig.aid)
        assertNull(HostCardEmulationConfig.pin)
        assertNull(HostCardEmulationConfig.eventSink)
        assertFalse(HostCardEmulationConfig.isConfigured)
    }
}
