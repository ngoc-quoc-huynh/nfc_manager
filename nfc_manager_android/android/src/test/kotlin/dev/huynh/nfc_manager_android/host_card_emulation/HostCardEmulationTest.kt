package dev.huynh.nfc_manager_android.host_card_emulation

import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.models.ResponseApdu
import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class HostCardEmulationTest {
    private val mockEventSink = mock<EventSink>()
    private val hostCardEmulation = HostCardEmulation()

    @BeforeEach
    fun setup() {
        HostCardEmulationConfig.configure(
            aid = byteArrayOf(),
            pin = byteArrayOf(),
            eventSink = mockEventSink,
        )
    }

    @Test
    fun `onListen configures HostCardEmulation correctly`() {
        hostCardEmulation.onListen(
            mapOf(
                "aid" to byteArrayOf(),
                "pin" to byteArrayOf(),
            ),
            mockEventSink,
        )

        assertTrue(HostCardEmulationConfig.isConfigured)
        verify(mockEventSink).success(HostCardEmulationStatus.READY.name)
    }

    @Test
    fun `onCancel clears HostCardEmulationConfig`() {
        hostCardEmulation.onCancel(null)

        assertFalse(HostCardEmulationConfig.isConfigured)
    }

    @Test
    fun `processCommandApdu emits HCE_NOT_READY when it is not configured`() {
        HostCardEmulationConfig.clear()
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = byteArrayOf(),
                extras = null,
            )

        assertContentEquals(ResponseApdu.HCE_NOT_READY(), response)
    }

    @Test
    fun `processCommandApdu emits NULL_COMMAND when command is null`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = null,
                extras = null,
            )

        assertContentEquals(ResponseApdu.NULL_COMMAND(), response)
    }

    @Test
    fun `processCommandApdu emits AID_SELECTED when command is SELECT command with correct AID`() {
        HostCardEmulationConfig.configure(
            aid = byteArrayOf(0x00),
            pin = byteArrayOf(),
            eventSink = mockEventSink,
        )
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray =
                    byteArrayOf(
                        0x00,
                        0xA4.toByte(),
                        0x04,
                        0x00,
                        0x01,
                        0x00,
                    ),
                extras = null,
            )

        assertContentEquals(ResponseApdu.OK(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.AID_SELECTED.name)
    }

    @Test
    fun `processCommandApdu emits INVALID_AID when command is SELECT command with wrong AID`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray =
                    byteArrayOf(
                        0x00,
                        0xA4.toByte(),
                        0x04,
                        0x00,
                        0x01,
                        0x00,
                    ),
                extras = null,
            )

        assertContentEquals(ResponseApdu.INVALID_AID(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.INVALID_AID.name)
    }

    @Test
    fun `processCommandApdu emits PIN_VERIFIED when command is VERIFY command with correct PIN`() {
        HostCardEmulationConfig.configure(
            aid = byteArrayOf(),
            pin = byteArrayOf(0x00),
            eventSink = mockEventSink,
        )
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray =
                    byteArrayOf(
                        0x00,
                        0x20,
                        0x00,
                        0x00,
                        0x01,
                        0x00,
                    ),
                extras = null,
            )

        assertContentEquals(ResponseApdu.OK(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.PIN_VERIFIED.name)
    }

    @Test
    fun `processCommandApdu emits WRONG_PIN when command is VERIFY command with wrong PIN`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray =
                    byteArrayOf(
                        0x00,
                        0x20,
                        0x00,
                        0x00,
                        0x01,
                        0x00,
                    ),
                extras = null,
            )

        assertContentEquals(ResponseApdu.WRONG_PIN(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.WRONG_PIN.name)
    }

    @Test
    fun `processCommandApdu emits FUNCTION_NOT_SUPPORTED when is not supported`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray =
                    byteArrayOf(
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                    ),
                extras = null,
            )

        assertContentEquals(ResponseApdu.FUNCTION_NOT_SUPPORTED(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.FUNCTION_NOT_SUPPORTED.name)
    }

    @Test
    fun `processCommandApdu emits INVALID_APDU_COMMAND when InvalidApduCommandException is thrown`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = byteArrayOf(),
                extras = null,
            )

        assertContentEquals(ResponseApdu.WRONG_LENGTH(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.INVALID_COMMAND.name)
    }

    @Test
    fun `processCommandApdu emits UNKNOWN when NfcException is thrown`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray =
                    byteArrayOf(
                        0x00,
                        0x00,
                        0x00,
                        0x00,
                        0x02,
                        0x00,
                    ),
                extras = null,
            )

        assertContentEquals(ResponseApdu.WRONG_LC_LENGTH(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.INVALID_LC_LENGTH.name)
    }

    @Test
    fun `onDeactivated emits TAG_DISCONNECTED`() {
        hostCardEmulation.onDeactivated(0)

        verify(mockEventSink).success(HostCardEmulationStatus.TAG_DISCONNECTED.name)
    }
}
