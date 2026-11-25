package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.InvalidHceArgumentsException
import dev.huynh.nfc_manager_android.error
import dev.huynh.nfc_manager_android.hce.HostCardEmulationSession.PinVerificationResult
import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HostCardEmulationStreamHandlerTest {
    private val mockEventSink = mock<EventSink>()
    private val streamHandler = HostCardEmulationStreamHandler()

    @AfterEach
    fun tearDown() = HostCardEmulationSessionHolder.clear(streamHandler)

    @Test
    fun `onListen starts a session owned by the stream handler at both length limits`() =
        listOf(
            VALID_AID to byteArrayOf(0x01),
            VALID_AID.copyOf(16) to ByteArray(255) { it.toByte() },
        ).forEach { (aid, pin) ->
            val sink = mock<EventSink>()

            streamHandler.onListen(mapOf("aid" to aid, "pin" to pin), sink)

            val session = assertNotNull(HostCardEmulationSessionHolder.active)
            assertSame(streamHandler, session.owner)
            assertTrue(session.matchesAid(aid))
            assertEquals(PinVerificationResult.VERIFIED, session.verifyPin(pin))
            verify(sink).success(HostCardEmulationStatus.READY.name)
        }

    @Test
    fun `onListen rejects invalid arguments and ends the stream`() =
        listOf(
            null,
            listOf(VALID_AID),
            mapOf("pin" to VALID_PIN),
            mapOf("aid" to VALID_AID),
            mapOf("aid" to null, "pin" to VALID_PIN),
            mapOf("aid" to VALID_AID, "pin" to null),
            mapOf("aid" to "01", "pin" to VALID_PIN),
            mapOf("aid" to VALID_AID, "pin" to 2),
            mapOf("aid" to ByteArray(4), "pin" to VALID_PIN),
            mapOf("aid" to ByteArray(17), "pin" to VALID_PIN),
            mapOf("aid" to VALID_AID, "pin" to ByteArray(0)),
            mapOf("aid" to VALID_AID, "pin" to ByteArray(256)),
        ).forEach { arguments ->
            assertRejected(streamHandler, arguments)
            assertNull(HostCardEmulationSessionHolder.active)
        }

    @Test
    fun `onListen with invalid arguments keeps the active session`() {
        streamHandler.onListen(VALID_ARGUMENTS, mockEventSink)
        val session = HostCardEmulationSessionHolder.active

        assertRejected(HostCardEmulationStreamHandler(), mapOf("aid" to ByteArray(4), "pin" to VALID_PIN))

        assertSame(session, assertNotNull(HostCardEmulationSessionHolder.active))
    }

    @Test
    fun `onListen replaces a session owned by another stream handler`() {
        val replacedSink = mock<EventSink>()
        HostCardEmulationStreamHandler().onListen(VALID_ARGUMENTS, replacedSink)

        streamHandler.onListen(VALID_ARGUMENTS, mockEventSink)

        assertSame(streamHandler, HostCardEmulationSessionHolder.active?.owner)
        verify(replacedSink).success(HostCardEmulationStatus.READY.name)
        verifyNoMoreInteractions(replacedSink)
    }

    @Test
    fun `onCancel clears the session it owns`() {
        streamHandler.onListen(VALID_ARGUMENTS, mockEventSink)

        streamHandler.onCancel(null)

        assertNull(HostCardEmulationSessionHolder.active)
    }

    @Test
    fun `onCancel keeps a session owned by another stream handler`() {
        streamHandler.onListen(VALID_ARGUMENTS, mockEventSink)

        HostCardEmulationStreamHandler().onCancel(null)

        assertSame(streamHandler, HostCardEmulationSessionHolder.active?.owner)
    }

    /** Asserts that [handler] reports [InvalidHceArgumentsException] for [arguments] and ends the stream. */
    private fun assertRejected(
        handler: HostCardEmulationStreamHandler,
        arguments: Any?,
    ) {
        val sink = mock<EventSink>()

        handler.onListen(arguments, sink)

        inOrder(sink) {
            verify(sink).error(InvalidHceArgumentsException())
            verify(sink).endOfStream()
        }
        verifyNoMoreInteractions(sink)
    }

    private companion object {
        val VALID_AID = byteArrayOf(0xF0.toByte(), 0x01, 0x02, 0x03, 0x04)
        val VALID_PIN = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val VALID_ARGUMENTS = mapOf("aid" to VALID_AID, "pin" to VALID_PIN)
    }
}
