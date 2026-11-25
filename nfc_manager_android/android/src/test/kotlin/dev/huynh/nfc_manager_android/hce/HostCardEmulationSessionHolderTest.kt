package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.PinBlockedException
import dev.huynh.nfc_manager_android.error
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import kotlin.test.assertNull
import kotlin.test.assertSame

class HostCardEmulationSessionHolderTest {
    private val owner = mock<StreamHandler>()
    private val sink = mock<EventSink>()
    private val session = newSession(sink)

    @AfterEach
    fun tearDown() = HostCardEmulationSessionHolder.clear(owner)

    @Test
    fun `start sets the session`() {
        HostCardEmulationSessionHolder.start(session)

        assertSame(session, HostCardEmulationSessionHolder.active)
    }

    @Test
    fun `clear by the owner resets the session`() {
        HostCardEmulationSessionHolder.start(session)

        HostCardEmulationSessionHolder.clear(owner)

        assertNull(HostCardEmulationSessionHolder.active)
    }

    @Test
    fun `clear by another owner keeps the session`() {
        HostCardEmulationSessionHolder.start(session)

        HostCardEmulationSessionHolder.clear(mock<StreamHandler>())

        assertSame(session, HostCardEmulationSessionHolder.active)
    }

    @Test
    fun `end of the active session clears it and ends its stream with the error`() {
        HostCardEmulationSessionHolder.start(session)

        HostCardEmulationSessionHolder.end(session, PinBlockedException())

        assertNull(HostCardEmulationSessionHolder.active)
        inOrder(sink) {
            verify(sink).error(PinBlockedException())
            verify(sink).endOfStream()
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `end of a stale session does not clear or notify its replacement`() {
        val replacementSink = mock<EventSink>()
        val replacement = newSession(replacementSink)
        HostCardEmulationSessionHolder.start(session)
        HostCardEmulationSessionHolder.start(replacement)

        HostCardEmulationSessionHolder.end(session, PinBlockedException())

        assertSame(replacement, HostCardEmulationSessionHolder.active)
        verifyNoInteractions(sink, replacementSink)
    }

    private fun newSession(sink: EventSink) = HostCardEmulationSession(owner = owner, aid = byteArrayOf(), pin = byteArrayOf(), sink = sink)
}
