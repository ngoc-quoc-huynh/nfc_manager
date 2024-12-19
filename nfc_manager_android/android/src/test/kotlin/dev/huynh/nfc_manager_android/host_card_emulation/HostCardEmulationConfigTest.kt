package dev.huynh.nfc_manager_android.host_card_emulation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class HostCardEmulationConfigTest {
    @BeforeEach
    fun setup() = HostCardEmulationConfig.clear()

    @Test
    fun `initial state aid and pin are null`() {
        assertNull(HostCardEmulationConfig.aid)
        assertNull(HostCardEmulationConfig.pin)
        assertFalse(HostCardEmulationConfig.isConfigured)
    }

    @Test
    fun `configure sets aid and pin`() {
        val aid = byteArrayOf()
        val pin = byteArrayOf()
        HostCardEmulationConfig.configure(aid, pin)

        assertContentEquals(aid, HostCardEmulationConfig.aid)
        assertContentEquals(pin, HostCardEmulationConfig.pin)
        assertTrue(HostCardEmulationConfig.isConfigured)
    }

    @Test
    fun `clear resets aid and pin`() {
        HostCardEmulationConfig.configure(
            aid = byteArrayOf(),
            pin = byteArrayOf(),
        )
        HostCardEmulationConfig.clear()

        assertNull(HostCardEmulationConfig.aid)
        assertNull(HostCardEmulationConfig.pin)
        assertFalse(HostCardEmulationConfig.isConfigured)
    }
}
