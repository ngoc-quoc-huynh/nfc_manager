package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.InvalidHceArgumentsException
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class HostCardEmulationStreamHandlerTest {
    private companion object {
        val VALID_AID = byteArrayOf(0xF0.toByte(), 0x01, 0x02, 0x03, 0x04)
        val VALID_PIN = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val VALID_ARGUMENTS = mapOf("aid" to VALID_AID, "pin" to VALID_PIN)
    }

    private val mockEventSink = mock<EventSink>()
    private val streamHandler = HostCardEmulationStreamHandler()

    @AfterEach
    fun tearDown() = HostCardEmulationConfig.clear(streamHandler)

    @Test
    fun `onListen starts a session owned by the stream handler`() {
        streamHandler.onListen(VALID_ARGUMENTS, mockEventSink)

        val session = assertNotNull(HostCardEmulationConfig.session)
        assertSame(streamHandler, session.owner)
        assertContentEquals(VALID_AID, session.aid)
        assertContentEquals(VALID_PIN, session.pin)
        assertSame(mockEventSink, session.sink)
        verify(mockEventSink).success(HostCardEmulationStatus.READY.name)
    }

    @Test
    fun `onListen accepts a 16 byte aid and a 255 byte pin`() {
        val aid = VALID_AID.copyOf(16)
        val pin = ByteArray(255) { it.toByte() }
        streamHandler.onListen(mapOf("aid" to aid, "pin" to pin), mockEventSink)

        val session = assertNotNull(HostCardEmulationConfig.session)
        assertContentEquals(aid, session.aid)
        assertContentEquals(pin, session.pin)
        verify(mockEventSink).success(HostCardEmulationStatus.READY.name)
    }

    @Test
    fun `onListen rejects null arguments`() = assertRejected(null)

    @Test
    fun `onListen rejects arguments that are not a map`() = assertRejected(listOf(VALID_AID))

    @Test
    fun `onListen rejects a missing aid`() = assertRejected(mapOf("pin" to VALID_PIN))

    @Test
    fun `onListen rejects a missing pin`() = assertRejected(mapOf("aid" to VALID_AID))

    @Test
    fun `onListen rejects a null aid`() = assertRejected(mapOf("aid" to null, "pin" to VALID_PIN))

    @Test
    fun `onListen rejects a null pin`() = assertRejected(mapOf("aid" to VALID_AID, "pin" to null))

    @Test
    fun `onListen rejects a wrongly typed aid`() = assertRejected(mapOf("aid" to "01", "pin" to VALID_PIN))

    @Test
    fun `onListen rejects a wrongly typed pin`() = assertRejected(mapOf("aid" to VALID_AID, "pin" to 2))

    @Test
    fun `onListen rejects a short aid`() = assertRejected(mapOf("aid" to ByteArray(4), "pin" to VALID_PIN))

    @Test
    fun `onListen rejects a long aid`() = assertRejected(mapOf("aid" to ByteArray(17), "pin" to VALID_PIN))

    @Test
    fun `onListen rejects an empty pin`() = assertRejected(mapOf("aid" to VALID_AID, "pin" to ByteArray(0)))

    @Test
    fun `onListen rejects a long pin`() = assertRejected(mapOf("aid" to VALID_AID, "pin" to ByteArray(256)))

    @Test
    fun `onCancel clears the session it owns`() {
        streamHandler.onListen(VALID_ARGUMENTS, mockEventSink)

        streamHandler.onCancel(null)

        assertNull(HostCardEmulationConfig.session)
    }

    @Test
    fun `onCancel keeps a session owned by another stream handler`() {
        streamHandler.onListen(VALID_ARGUMENTS, mockEventSink)

        HostCardEmulationStreamHandler().onCancel(null)

        assertSame(streamHandler, HostCardEmulationConfig.session?.owner)
    }

    /**
     * Asserts that [HostCardEmulationStreamHandler.onListen] reports [InvalidHceArgumentsException]
     * for [arguments] and ends the stream without starting a session.
     */
    private fun assertRejected(arguments: Any?) {
        streamHandler.onListen(arguments, mockEventSink)

        assertNull(HostCardEmulationConfig.session)
        inOrder(mockEventSink) {
            verify(mockEventSink).error(
                "INVALID_ARGUMENTS",
                "HCE requires an AID of 5–16 bytes and a PIN of 1–255 bytes.",
                null,
            )
            verify(mockEventSink).endOfStream()
        }
        verifyNoMoreInteractions(mockEventSink)
    }
}
