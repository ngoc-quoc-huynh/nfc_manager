package dev.huynh.nfc_manager_android.host_card_emulation

import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.models.ResponseApdu
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.assertContentEquals

class HostCardEmulationTest {
    private companion object {
        val VALID_AID = byteArrayOf(0xF0.toByte(), 0x01, 0x02, 0x03, 0x04)
        val VALID_PIN = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val OTHER_AID = byteArrayOf(0xF0.toByte(), 0x01, 0x02, 0x03, 0x05)
        val SELECT_HEADER = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        val VERIFY_HEADER = byteArrayOf(0x00, 0x20, 0x00, 0x00)

        fun apdu(
            header: ByteArray,
            data: ByteArray,
            le: Byte? = null,
        ): ByteArray = header + data.size.toByte() + data + (le?.let { byteArrayOf(it) } ?: byteArrayOf())
    }

    private val mockEventSink = mock<EventSink>()
    private val owner = mock<StreamHandler>()
    private val hostCardEmulation = HostCardEmulation()

    @BeforeEach
    fun setup() = startSession()

    @AfterEach
    fun tearDown() = HostCardEmulationConfig.clear(owner)

    @Test
    fun `processCommandApdu emits HCE_NOT_READY when it is not configured`() {
        HostCardEmulationConfig.clear(owner)
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = byteArrayOf(),
                extras = null,
            )

        assertContentEquals(ResponseApdu.HCE_NOT_READY.toByteArray(), response)
    }

    @Test
    fun `processCommandApdu emits NULL_COMMAND when command is null`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = null,
                extras = null,
            )

        assertContentEquals(ResponseApdu.NULL_COMMAND.toByteArray(), response)
    }

    @Test
    fun `processCommandApdu emits AID_SELECTED when command is SELECT command with correct AID`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = apdu(SELECT_HEADER, VALID_AID),
                extras = null,
            )

        assertContentEquals(ResponseApdu.OK.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.AID_SELECTED.name)
    }

    @Test
    fun `processCommandApdu emits INVALID_AID when command is SELECT command with wrong AID`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = apdu(SELECT_HEADER, OTHER_AID),
                extras = null,
            )

        assertContentEquals(ResponseApdu.INVALID_AID.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.INVALID_AID.name)
    }

    @Test
    fun `processCommandApdu emits PIN_VERIFIED when command is VERIFY command with correct PIN`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = apdu(VERIFY_HEADER, VALID_PIN),
                extras = null,
            )

        assertContentEquals(ResponseApdu.OK.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.PIN_VERIFIED.name)
    }

    @Test
    fun `processCommandApdu emits WRONG_PIN when command is VERIFY command with wrong PIN`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = apdu(VERIFY_HEADER, byteArrayOf(0x09, 0x09, 0x09, 0x09)),
                extras = null,
            )

        assertContentEquals(ResponseApdu.WRONG_PIN.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.WRONG_PIN.name)
    }

    @Test
    fun `processCommandApdu emits WRONG_PIN when VERIFY PIN differs in the last byte`() {
        startSession(pin = byteArrayOf(0x01, 0x02, 0x03, 0x04))
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = byteArrayOf(0x00, 0x20, 0x00, 0x00, 0x04, 0x01, 0x02, 0x03, 0x05),
                extras = null,
            )

        assertContentEquals(ResponseApdu.WRONG_PIN.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.WRONG_PIN.name)
    }

    @Test
    fun `processCommandApdu emits WRONG_PIN when VERIFY PIN is a prefix of the PIN`() {
        startSession(pin = byteArrayOf(0x01, 0x02, 0x03, 0x04))
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = byteArrayOf(0x00, 0x20, 0x00, 0x00, 0x02, 0x01, 0x02),
                extras = null,
            )

        assertContentEquals(ResponseApdu.WRONG_PIN.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.WRONG_PIN.name)
    }

    @Test
    fun `processCommandApdu rejects SELECT and VERIFY commands without data`() {
        val headers =
            listOf(
                byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00),
                byteArrayOf(0x00, 0x20, 0x00, 0x00),
            )
        val suffixes =
            listOf(
                byteArrayOf(),
                byteArrayOf(0x00),
                byteArrayOf(0x00, 0x00),
            )

        for (header in headers) {
            for (suffix in suffixes) {
                val command = header + suffix
                val response = hostCardEmulation.processCommandApdu(command, null)

                assertContentEquals(
                    byteArrayOf(0x67, 0x00),
                    response,
                    "Command: ${command.contentToString()}",
                )
            }
        }

        verify(mockEventSink, times(headers.size * suffixes.size))
            .success(HostCardEmulationStatus.INVALID_LC_LENGTH.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `processCommandApdu emits FUNCTION_NOT_SUPPORTED when the instruction is neither SELECT nor VERIFY`() {
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

        assertContentEquals(ResponseApdu.FUNCTION_NOT_SUPPORTED.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.FUNCTION_NOT_SUPPORTED.name)
    }

    @Test
    fun `processCommandApdu answers 67 00 and emits INVALID_LC_LENGTH for a 6-byte command with Lc 00`() {
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
                extras = null,
            )

        assertContentEquals(byteArrayOf(0x67, 0x00), response)
        verify(mockEventSink).success(HostCardEmulationStatus.INVALID_LC_LENGTH.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `processCommandApdu emits AID_SELECTED when SELECT carries a trailing Le`() {
        val response =
            hostCardEmulation.processCommandApdu(apdu(SELECT_HEADER, VALID_AID, le = 0x00), null)

        assertContentEquals(ResponseApdu.OK.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.AID_SELECTED.name)
    }

    @Test
    fun `processCommandApdu emits PIN_VERIFIED for a 255-byte PIN`() {
        val pin = ByteArray(255) { it.toByte() }
        startSession(pin = pin)

        val response = hostCardEmulation.processCommandApdu(apdu(VERIFY_HEADER, pin), null)

        assertContentEquals(ResponseApdu.OK.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.PIN_VERIFIED.name)
    }

    @Test
    fun `processCommandApdu emits PIN_VERIFIED for a 255-byte PIN followed by Le`() {
        val pin = ByteArray(255) { it.toByte() }
        startSession(pin = pin)

        val response = hostCardEmulation.processCommandApdu(apdu(VERIFY_HEADER, pin, le = 0x00), null)

        assertContentEquals(ResponseApdu.OK.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.PIN_VERIFIED.name)
    }

    @Test
    fun `processCommandApdu emits WRONG_PIN for a 255-byte PIN differing in the last byte`() {
        val pin = ByteArray(255) { it.toByte() }
        startSession(pin = pin)
        val attempt = pin.copyOf().also { it[254] = (it[254] + 1).toByte() }

        val response = hostCardEmulation.processCommandApdu(apdu(VERIFY_HEADER, attempt, le = 0x00), null)

        assertContentEquals(ResponseApdu.WRONG_PIN.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.WRONG_PIN.name)
    }

    @Test
    fun `processCommandApdu rejects extended-length SELECT and VERIFY commands`() {
        // Extended-length APDUs (Lc = 00 followed by a two-byte length) are not supported.
        val commands =
            listOf(
                SELECT_HEADER + byteArrayOf(0x00, 0x00, VALID_AID.size.toByte()) + VALID_AID,
                VERIFY_HEADER + byteArrayOf(0x00, 0x00, VALID_PIN.size.toByte()) + VALID_PIN,
                VERIFY_HEADER + byteArrayOf(0x00, 0x01, 0x00) + ByteArray(256),
            )

        for (command in commands) {
            val response = hostCardEmulation.processCommandApdu(command, null)

            assertContentEquals(byteArrayOf(0x67, 0x00), response, "Command: ${command.contentToString()}")
        }
        verify(mockEventSink, times(commands.size)).success(HostCardEmulationStatus.INVALID_LC_LENGTH.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `processCommandApdu rejects a command whose data is shorter than Lc`() {
        val command = SELECT_HEADER + byteArrayOf(VALID_AID.size.toByte()) + VALID_AID.copyOf(VALID_AID.size - 2)

        val response = hostCardEmulation.processCommandApdu(command, null)

        assertContentEquals(byteArrayOf(0x67, 0x00), response)
        verify(mockEventSink).success(HostCardEmulationStatus.INVALID_LC_LENGTH.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `processCommandApdu answers 67 00 and emits INVALID_COMMAND for commands shorter than 4 bytes`() {
        for (size in 0..3) {
            val response =
                hostCardEmulation.processCommandApdu(
                    commandByteArray = ByteArray(size),
                    extras = null,
                )

            assertContentEquals(byteArrayOf(0x67, 0x00), response, "command of $size bytes")
        }
        verify(mockEventSink, times(4)).success(HostCardEmulationStatus.INVALID_COMMAND.name)
    }

    @Test
    fun `processCommandApdu answers 67 00 and emits INVALID_LC_LENGTH when Lc does not match the data length`() {
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

        assertContentEquals(byteArrayOf(0x67, 0x00), response)
        verify(mockEventSink).success(HostCardEmulationStatus.INVALID_LC_LENGTH.name)
    }

    @Test
    fun `onDeactivated emits TAG_DISCONNECTED`() {
        hostCardEmulation.onDeactivated(0)

        verify(mockEventSink).success(HostCardEmulationStatus.TAG_DISCONNECTED.name)
    }

    @Test
    fun `onDeactivated emits nothing when it is not configured`() {
        HostCardEmulationConfig.clear(owner)

        hostCardEmulation.onDeactivated(0)

        verifyNoInteractions(mockEventSink)
    }

    private fun startSession(
        aid: ByteArray = VALID_AID,
        pin: ByteArray = VALID_PIN,
    ) = HostCardEmulationConfig.start(
        HostCardEmulationConfig.Session(
            owner = owner,
            aid = aid,
            pin = pin,
            sink = mockEventSink,
        ),
    )
}
