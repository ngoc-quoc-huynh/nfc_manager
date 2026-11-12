package dev.huynh.nfc_manager_android.host_card_emulation

import dev.huynh.nfc_manager_android.InvalidArgumentsException
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.models.ResponseApdu
import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class HostCardEmulationTest {
    private val mockEventSink = mock<EventSink>()
    private val hostCardEmulation = HostCardEmulation()

    @BeforeEach
    fun setup() = startSession()

    @AfterEach
    fun tearDown() = HostCardEmulationConfig.clear(hostCardEmulation)

    @Test
    fun `onListen starts a session owned by the stream handler`() {
        HostCardEmulationConfig.clear(hostCardEmulation)
        val aid = byteArrayOf(0x01)
        val pin = byteArrayOf(0x02)

        hostCardEmulation.onListen(mapOf("aid" to aid, "pin" to pin), mockEventSink)

        val session = assertNotNull(HostCardEmulationConfig.session)
        assertSame(hostCardEmulation, session.owner)
        assertContentEquals(aid, session.aid)
        assertContentEquals(pin, session.pin)
        assertSame(mockEventSink, session.sink)
        verify(mockEventSink).success(HostCardEmulationStatus.READY.name)
    }

    @Test
    fun `onListen rejects null arguments`() = assertRejected(null)

    @Test
    fun `onListen rejects arguments that are not a map`() = assertRejected(listOf(byteArrayOf(0x01)))

    @Test
    fun `onListen rejects a missing aid`() = assertRejected(mapOf("pin" to byteArrayOf(0x02)))

    @Test
    fun `onListen rejects a missing pin`() = assertRejected(mapOf("aid" to byteArrayOf(0x01)))

    @Test
    fun `onListen rejects a null aid`() = assertRejected(mapOf("aid" to null, "pin" to byteArrayOf(0x02)))

    @Test
    fun `onListen rejects a null pin`() = assertRejected(mapOf("aid" to byteArrayOf(0x01), "pin" to null))

    @Test
    fun `onListen rejects a wrongly typed aid`() = assertRejected(mapOf("aid" to "01", "pin" to byteArrayOf(0x02)))

    @Test
    fun `onListen rejects a wrongly typed pin`() = assertRejected(mapOf("aid" to byteArrayOf(0x01), "pin" to 2))

    @Test
    fun `onCancel clears the session it owns`() {
        hostCardEmulation.onCancel(null)

        assertNull(HostCardEmulationConfig.session)
    }

    @Test
    fun `onCancel keeps a session owned by another stream handler`() {
        HostCardEmulation().onCancel(null)

        assertNotNull(HostCardEmulationConfig.session)
    }

    @Test
    fun `processCommandApdu emits HCE_NOT_READY when it is not configured`() {
        HostCardEmulationConfig.clear(hostCardEmulation)
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
        startSession(aid = byteArrayOf(0x00))
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
        startSession(pin = byteArrayOf(0x00))
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
    fun `processCommandApdu emits WRONG_PIN when VERIFY PIN differs in the last byte`() {
        startSession(pin = byteArrayOf(0x01, 0x02, 0x03, 0x04))
        val response =
            hostCardEmulation.processCommandApdu(
                commandByteArray = byteArrayOf(0x00, 0x20, 0x00, 0x00, 0x04, 0x01, 0x02, 0x03, 0x05),
                extras = null,
            )

        assertContentEquals(ResponseApdu.WRONG_PIN(), response)
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

        assertContentEquals(ResponseApdu.WRONG_PIN(), response)
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
        verifyNoMoreInteractions(mockEventSink)
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
        HostCardEmulationConfig.clear(hostCardEmulation)

        hostCardEmulation.onDeactivated(0)

        verifyNoInteractions(mockEventSink)
    }

    /**
     * Asserts that [onListen] reports [InvalidArgumentsException] for [arguments] and leaves HCE
     * unconfigured.
     */
    private fun assertRejected(arguments: Any?) {
        HostCardEmulationConfig.clear(hostCardEmulation)

        hostCardEmulation.onListen(arguments, mockEventSink)

        assertNull(HostCardEmulationConfig.session)
        inOrder(mockEventSink) {
            verify(mockEventSink).error(
                "INVALID_ARGUMENTS",
                "The arguments are missing or have the wrong type.",
                null,
            )
            verify(mockEventSink).endOfStream()
        }
        verifyNoMoreInteractions(mockEventSink)
    }

    private fun startSession(
        aid: ByteArray = byteArrayOf(),
        pin: ByteArray = byteArrayOf(),
    ) = HostCardEmulationConfig.start(
        HostCardEmulationConfig.Session(
            owner = hostCardEmulation,
            aid = aid,
            pin = pin,
            sink = mockEventSink,
        ),
    )
}
