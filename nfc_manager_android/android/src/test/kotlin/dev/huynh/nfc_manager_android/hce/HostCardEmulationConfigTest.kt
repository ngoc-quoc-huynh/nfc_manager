package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.PinBlockedException
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import kotlin.test.assertNull
import kotlin.test.assertSame

class HostCardEmulationConfigTest {
    private val owner = mock<StreamHandler>()
    private val session =
        HostCardEmulationConfig.Session(
            owner = owner,
            aid = byteArrayOf(),
            pin = byteArrayOf(),
            sink = mock<EventSink>(),
        )

    @AfterEach
    fun tearDown() = HostCardEmulationConfig.clear(owner)

    @Test
    fun `initial state has no session`() {
        assertNull(HostCardEmulationConfig.session)
    }

    @Test
    fun `start sets the session`() {
        HostCardEmulationConfig.start(session)

        assertSame(session, HostCardEmulationConfig.session)
    }

    @Test
    fun `clear by the owner resets the session`() {
        HostCardEmulationConfig.start(session)

        HostCardEmulationConfig.clear(owner)

        assertNull(HostCardEmulationConfig.session)
    }

    @Test
    fun `clear by another owner keeps the session`() {
        HostCardEmulationConfig.start(session)

        HostCardEmulationConfig.clear(mock<StreamHandler>())

        assertSame(session, HostCardEmulationConfig.session)
    }

    @Test
    fun `end of a stale session does not clear or notify its replacement`() {
        val replacementSink = mock<EventSink>()
        val replacement =
            HostCardEmulationConfig.Session(
                owner = owner,
                aid = byteArrayOf(),
                pin = byteArrayOf(),
                sink = replacementSink,
            )
        HostCardEmulationConfig.start(session)
        HostCardEmulationConfig.start(replacement)

        HostCardEmulationConfig.end(session, PinBlockedException())

        assertSame(replacement, HostCardEmulationConfig.session)
        verifyNoInteractions(session.sink, replacementSink)
    }
}
