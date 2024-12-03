package dev.huynh.nfc_manager_android

import android.app.Activity
import android.nfc.NfcAdapter
import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.test.assertFalse
import kotlin.test.assertNull

class NfcReaderTest {
    private val mockActivity = mock<Activity>()
    private val mockEventSink = mock<EventSink>()
    private val mockAdapter = mock<NfcAdapter>()
    private val nfcReader =
        NfcReader(
            activity = mockActivity,
            nfcAdapter = mockAdapter,
        )

    @Test
    fun `startDiscovery should throw NfcNotSupportedException when nfcAdapter is null`() {
        val nfcReader = NfcReader(activity = mockActivity, nfcAdapter = null)
        assertThrows<NfcNotSupportedException> { nfcReader.startDiscovery() }
    }

    @Test
    fun `startDiscovery should not start a new discovery when discovery is already started`() {
        nfcReader.eventSink = mockEventSink
        nfcReader.isDiscoveryStarted = true
        nfcReader.startDiscovery()

        verify(mockAdapter, times(0)).enableReaderMode(
            eq(mockActivity),
            any(),
            eq(NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK),
            eq(null),
        )
    }

    @Test
    fun `startDiscovery should start discovery when eventSink is available and discovery is not started`() {
        nfcReader.eventSink = mockEventSink
        nfcReader.startDiscovery()

        verify(mockAdapter).enableReaderMode(
            eq(mockActivity),
            any(),
            eq(NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK),
            eq(null),
        )
    }

    @Test
    fun `stopDiscovery should not stop discovery when it is already stopped`() {
        nfcReader.stopDiscovery()

        verify(mockAdapter, times(0))
            .disableReaderMode(any())
    }

    @Test
    fun `stopDiscovery should stop discovery when it is started`() {
        nfcReader.isDiscoveryStarted = true
        nfcReader.stopDiscovery()

        verify(mockAdapter).disableReaderMode(mockActivity)
    }

    @Test
    fun `stopDiscovery should throw NfcNotSupportedException when nfcAdapter is null`() {
        val nfcReader = NfcReader(activity = mockActivity, nfcAdapter = null)
        assertThrows<NfcNotSupportedException> { nfcReader.stopDiscovery() }
    }

    @Test
    fun `onListen should set eventSink correctly`() {
        nfcReader.onListen(null, mockEventSink)

        assertEquals(mockEventSink, nfcReader.eventSink)
    }

    @Test
    fun `onCancel should stop discovery and nullify eventSink`() {
        nfcReader.eventSink = mockEventSink
        nfcReader.isDiscoveryStarted = true
        nfcReader.onCancel(null)

        assertNull(nfcReader.eventSink)
        assertFalse(nfcReader.isDiscoveryStarted)
    }
}
