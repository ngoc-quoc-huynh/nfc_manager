package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.PinBlockedException
import dev.huynh.nfc_manager_android.error
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.assertContentEquals

class HostCardEmulationServiceTest {
    private val mockEventSink = mock<EventSink>()
    private val owner = mock<StreamHandler>()
    private val service = HostCardEmulationService()

    @BeforeEach
    fun setup() = startSession()

    @AfterEach
    fun tearDown() = HostCardEmulationSessionHolder.clear(owner)

    @Test
    fun `processCommandApdu answers 69 82 without a session`() {
        HostCardEmulationSessionHolder.clear(owner)

        assertResponse(ResponseApdu.HCE_NOT_READY, process(byteArrayOf()))
        verifyNoInteractions(mockEventSink)
    }

    @Test
    fun `processCommandApdu answers 6A 00 for a null command`() {
        assertResponse(ResponseApdu.NULL_COMMAND, process(null))
        verifyNoInteractions(mockEventSink)
    }

    @Test
    fun `SELECT answers 90 00 for the configured AID and 6A 82 for another AID`() =
        listOf(
            Triple(selectCommand(VALID_AID), ResponseApdu.OK, HostCardEmulationStatus.AID_SELECTED),
            Triple(selectCommand(VALID_AID, le = 0x00), ResponseApdu.OK, HostCardEmulationStatus.AID_SELECTED),
            Triple(selectCommand(OTHER_AID), ResponseApdu.INVALID_AID, HostCardEmulationStatus.INVALID_AID),
        ).forEach { (command, response, status) ->
            val sink = mock<EventSink>()
            startSession(sink = sink)

            assertResponse(response, process(command), command.contentToString())
            verify(sink).success(status.name)
        }

    @Test
    fun `VERIFY with the configured PIN answers 90 00 and emits PIN_VERIFIED`() =
        listOf(
            VALID_PIN to verifyCommand(VALID_PIN),
            LONG_PIN to verifyCommand(LONG_PIN),
            LONG_PIN to verifyCommand(LONG_PIN, le = 0x00),
        ).forEach { (pin, command) ->
            val sink = mock<EventSink>()
            startSession(pin = pin, sink = sink)
            selectConfiguredAid()

            assertResponse(ResponseApdu.OK, process(command), command.contentToString())
            verify(sink).success(HostCardEmulationStatus.PIN_VERIFIED.name)
        }

    @Test
    fun `VERIFY with another PIN answers 63 00 and emits WRONG_PIN`() =
        listOf(
            // Every byte differs, only the last byte differs, a prefix, and a long PIN followed by Le.
            VALID_PIN to WRONG_PIN_COMMAND,
            VALID_PIN to verifyCommand(byteArrayOf(0x01, 0x02, 0x03, 0x05)),
            VALID_PIN to verifyCommand(byteArrayOf(0x01, 0x02)),
            LONG_PIN to verifyCommand(LONG_PIN.copyOf().also { it[it.lastIndex]++ }, le = 0x00),
        ).forEach { (pin, command) ->
            val sink = mock<EventSink>()
            startSession(pin = pin, sink = sink)
            selectConfiguredAid()

            assertResponse(ResponseApdu.WRONG_PIN, process(command), command.contentToString())
            verify(sink).success(HostCardEmulationStatus.WRONG_PIN.name)
        }

    @Test
    fun `processCommandApdu answers 67 00 and emits INVALID_LC_LENGTH`() {
        val commands =
            listOf(
                // SELECT and VERIFY without data: no suffix, only Le, or Lc 00.
                SELECT_HEADER,
                SELECT_HEADER + byteArrayOf(0x00),
                SELECT_HEADER + byteArrayOf(0x00, 0x00),
                VERIFY_HEADER,
                VERIFY_HEADER + byteArrayOf(0x00),
                VERIFY_HEADER + byteArrayOf(0x00, 0x00),
                // Lc says 5 data bytes, but only 3 follow.
                SELECT_HEADER + byteArrayOf(0x05) + VALID_AID.copyOf(3),
                // Two bytes follow the data, one more than Le allows.
                selectCommand(VALID_AID) + byteArrayOf(0x00, 0x00),
                // An extended-length encoding: Lc 00, then a two-byte length.
                SELECT_HEADER + byteArrayOf(0x00, 0x00, VALID_AID.size.toByte()) + VALID_AID,
            )

        commands.forEach { assertResponse(ResponseApdu.WRONG_LENGTH, process(it), it.contentToString()) }

        verify(mockEventSink, times(commands.size)).success(HostCardEmulationStatus.INVALID_LC_LENGTH.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `processCommandApdu answers 67 00 and emits INVALID_COMMAND for commands shorter than 4 bytes`() {
        (0..3).forEach { size -> assertResponse(ResponseApdu.WRONG_LENGTH, process(ByteArray(size)), "$size bytes") }

        verify(mockEventSink, times(4)).success(HostCardEmulationStatus.INVALID_COMMAND.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `processCommandApdu emits FUNCTION_NOT_SUPPORTED and answers 6A 81 for unsupported commands`() =
        listOf(
            // An unknown instruction, then SELECT by file ID and VERIFY with P1 01.
            byteArrayOf(0x00, 0x00, 0x00, 0x00) to ResponseApdu.FUNCTION_NOT_SUPPORTED,
            byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x00) to ResponseApdu.FUNCTION_NOT_SUPPORTED,
            byteArrayOf(0x00, 0x20, 0x01, 0x00) to ResponseApdu.FUNCTION_NOT_SUPPORTED,
        ).forEach { (command, expected) ->
            val sink = mock<EventSink>()
            startSession(sink = sink)

            assertResponse(expected, process(command), command.contentToString())
            verify(sink).success(HostCardEmulationStatus.FUNCTION_NOT_SUPPORTED.name)
        }

    @Test
    fun `processCommandApdu blocks the PIN then ends the stream without stale events`() {
        selectConfiguredAid()

        repeat(2) { assertResponse(ResponseApdu.WRONG_PIN, process(WRONG_PIN_COMMAND)) }
        assertResponse(ResponseApdu.PIN_BLOCKED, process(WRONG_PIN_COMMAND))

        inOrder(mockEventSink) {
            verify(mockEventSink).success(HostCardEmulationStatus.AID_SELECTED.name)
            verify(mockEventSink, times(2)).success(HostCardEmulationStatus.WRONG_PIN.name)
            verify(mockEventSink).error(PinBlockedException())
            verify(mockEventSink).endOfStream()
        }
        service.onDeactivated(0)
        assertResponse(ResponseApdu.HCE_NOT_READY, process(verifyCommand(VALID_PIN)))
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `processCommandApdu keeps the wrong PIN count across deactivation and reselection`() {
        selectConfiguredAid()
        repeat(2) { process(WRONG_PIN_COMMAND) }
        service.onDeactivated(0)
        selectConfiguredAid()

        assertResponse(ResponseApdu.PIN_BLOCKED, process(WRONG_PIN_COMMAND))
        verify(mockEventSink).success(HostCardEmulationStatus.TAG_DISCONNECTED.name)
        verify(mockEventSink, times(2)).success(HostCardEmulationStatus.AID_SELECTED.name)
        verify(mockEventSink).error(PinBlockedException())
        verify(mockEventSink).endOfStream()
    }

    @Test
    fun `processCommandApdu resets the wrong PIN count after a correct PIN`() {
        selectConfiguredAid()

        repeat(2) { process(WRONG_PIN_COMMAND) }
        process(verifyCommand(VALID_PIN))
        repeat(2) { assertResponse(ResponseApdu.WRONG_PIN, process(WRONG_PIN_COMMAND)) }

        verify(mockEventSink, times(4)).success(HostCardEmulationStatus.WRONG_PIN.name)
        verify(mockEventSink, never()).endOfStream()
    }

    @Test
    fun `new session after lockout receives a full wrong PIN budget`() {
        selectConfiguredAid()
        repeat(2) { assertResponse(ResponseApdu.WRONG_PIN, process(WRONG_PIN_COMMAND)) }
        assertResponse(ResponseApdu.PIN_BLOCKED, process(WRONG_PIN_COMMAND))

        val restartedSink = mock<EventSink>()
        startSession(sink = restartedSink)
        selectConfiguredAid()
        repeat(2) { assertResponse(ResponseApdu.WRONG_PIN, process(WRONG_PIN_COMMAND)) }
        assertResponse(ResponseApdu.PIN_BLOCKED, process(WRONG_PIN_COMMAND))

        inOrder(restartedSink) {
            verify(restartedSink).success(HostCardEmulationStatus.AID_SELECTED.name)
            verify(restartedSink, times(2)).success(HostCardEmulationStatus.WRONG_PIN.name)
            verify(restartedSink).error(PinBlockedException())
            verify(restartedSink).endOfStream()
        }
        verifyNoMoreInteractions(restartedSink)
    }

    @Test
    fun `VERIFY before SELECT answers 69 85 without checking the PIN`() {
        assertResponse(ResponseApdu.AID_NOT_SELECTED, process(verifyCommand(VALID_PIN)))
        verifyNoInteractions(mockEventSink)
    }

    @Test
    fun `VERIFY before SELECT does not count as a wrong PIN`() {
        repeat(3) { process(WRONG_PIN_COMMAND) }

        selectConfiguredAid()
        repeat(2) { assertResponse(ResponseApdu.WRONG_PIN, process(WRONG_PIN_COMMAND)) }
        verify(mockEventSink, never()).endOfStream()
    }

    @Test
    fun `VERIFY after a SELECT for another AID answers 69 85`() {
        selectConfiguredAid()
        process(selectCommand(OTHER_AID))

        assertResponse(ResponseApdu.AID_NOT_SELECTED, process(verifyCommand(VALID_PIN)))
        verify(mockEventSink, never()).success(HostCardEmulationStatus.PIN_VERIFIED.name)
    }

    @Test
    fun `VERIFY after deactivation answers 69 85 until the AID is selected again`() {
        selectConfiguredAid()
        service.onDeactivated(0)

        assertResponse(ResponseApdu.AID_NOT_SELECTED, process(verifyCommand(VALID_PIN)))

        selectConfiguredAid()
        assertResponse(ResponseApdu.OK, process(verifyCommand(VALID_PIN)))
    }

    @Test
    fun `VERIFY after the session is replaced answers 69 85`() {
        selectConfiguredAid()
        startSession()

        assertResponse(ResponseApdu.AID_NOT_SELECTED, process(verifyCommand(VALID_PIN)))
    }

    @Test
    fun `onDeactivated emits TAG_DISCONNECTED once for an active session that handled an APDU`() {
        process(selectCommand(VALID_AID))

        service.onDeactivated(0)
        service.onDeactivated(0)

        verify(mockEventSink).success(HostCardEmulationStatus.AID_SELECTED.name)
        verify(mockEventSink).success(HostCardEmulationStatus.TAG_DISCONNECTED.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `onDeactivated emits TAG_DISCONNECTED after a malformed APDU`() {
        process(byteArrayOf())

        service.onDeactivated(0)

        verify(mockEventSink).success(HostCardEmulationStatus.INVALID_COMMAND.name)
        verify(mockEventSink).success(HostCardEmulationStatus.TAG_DISCONNECTED.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `onDeactivated emits nothing without an active session that handled an APDU`() =
        listOf<() -> Unit>(
            // No APDU, only a null command, and no session.
            {},
            { process(null) },
            { HostCardEmulationSessionHolder.clear(owner) },
        ).forEach { arrange ->
            val sink = mock<EventSink>()
            startSession(sink = sink)
            arrange()

            service.onDeactivated(0)

            verifyNoInteractions(sink)
        }

    @Test
    fun `onDeactivated does not emit TAG_DISCONNECTED when the session is replaced after an APDU`() {
        process(selectCommand(VALID_AID))
        startSession(aid = OTHER_AID)

        service.onDeactivated(0)

        verify(mockEventSink).success(HostCardEmulationStatus.AID_SELECTED.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `onDeactivated does not emit TAG_DISCONNECTED after the session is cancelled`() {
        process(selectCommand(VALID_AID))
        HostCardEmulationSessionHolder.clear(owner)

        service.onDeactivated(0)

        verify(mockEventSink).success(HostCardEmulationStatus.AID_SELECTED.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    private fun process(command: ByteArray?) = service.processCommandApdu(command, extras = null)

    private fun assertResponse(
        expected: ResponseApdu,
        actual: ByteArray,
        message: String? = null,
    ) = assertContentEquals(expected.toByteArray(), actual, message)

    private fun selectConfiguredAid() = assertResponse(ResponseApdu.OK, process(selectCommand(VALID_AID)))

    private fun startSession(
        aid: ByteArray = VALID_AID,
        pin: ByteArray = VALID_PIN,
        sink: EventSink = mockEventSink,
    ) = HostCardEmulationSessionHolder.start(HostCardEmulationSession(owner = owner, aid = aid, pin = pin, sink = sink))

    private companion object {
        val VALID_AID = byteArrayOf(0xF0.toByte(), 0x01, 0x02, 0x03, 0x04)
        val OTHER_AID = byteArrayOf(0xF0.toByte(), 0x01, 0x02, 0x03, 0x05)
        val VALID_PIN = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val LONG_PIN = ByteArray(255) { it.toByte() }
        val SELECT_HEADER = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        val VERIFY_HEADER = byteArrayOf(0x00, 0x20, 0x00, 0x00)
        val WRONG_PIN_COMMAND = verifyCommand(byteArrayOf(0x09, 0x09, 0x09, 0x09))

        fun selectCommand(
            aid: ByteArray,
            le: Byte? = null,
        ) = shortDataApdu(SELECT_HEADER, aid, le)

        fun verifyCommand(
            pin: ByteArray,
            le: Byte? = null,
        ) = shortDataApdu(VERIFY_HEADER, pin, le)

        private fun shortDataApdu(
            header: ByteArray,
            data: ByteArray,
            le: Byte?,
        ) = header + data.size.toByte() + data + listOfNotNull(le)
    }
}
