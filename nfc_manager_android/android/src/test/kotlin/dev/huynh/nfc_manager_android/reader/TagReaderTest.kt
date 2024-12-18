package dev.huynh.nfc_manager_android.reader

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import dev.huynh.nfc_manager_android.IsoDepNotSupportedException
import dev.huynh.nfc_manager_android.NfcNotSupportedException
import dev.huynh.nfc_manager_android.TagConnectionException
import dev.huynh.nfc_manager_android.TagReader
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import kotlin.test.assertContentEquals

class TagReaderTest {
    private val mockActivity = mock<Activity>()
    private val mockEventSink = mock<EventSink>()
    private val mockIsoDep = mock<IsoDep>()
    private val mockNfcAdapter = mock<NfcAdapter>()
    private val mockTag = mock<Tag>()
    private lateinit var tagReader: TagReader

    @BeforeEach
    fun setup() {
        tagReader =
            TagReader(
                activity = mockActivity,
                nfcAdapter = mockNfcAdapter,
            )
    }

    @Test
    fun `startDiscovery throws NfcNotSupportedException when nfcAdapter is null`() {
        val tagReader = TagReader(activity = mockActivity, nfcAdapter = null)
        assertThrows<NfcNotSupportedException> { tagReader.startDiscovery() }
    }

    @Test
    fun `startDiscovery should not start if already started`() {
        tagReader.isDiscoveryStarted = true
        tagReader.startDiscovery()

        verify(mockNfcAdapter, times(0)).enableReaderMode(
            eq(mockActivity),
            any(),
            eq(NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B),
            eq(null),
        )
    }

    @Test
    fun `startDiscovery should start if not already started`() {
        tagReader.isDiscoveryStarted = false
        tagReader.startDiscovery()

        verify(mockNfcAdapter).enableReaderMode(
            eq(mockActivity),
            any(),
            eq(NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B),
            eq(null),
        )
    }

    @Test
    fun `onTagFound emits success when IsoDep is available`() {
        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(mockIsoDep)
            whenever(mockTag.id).thenReturn(byteArrayOf(0x00))
            whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
                val runnable = invocation.getArgument<Runnable>(0)
                runnable.run()
                null
            }

            tagReader.eventSink = mockEventSink
            tagReader.onTagFound(mockTag)

            verify(mockIsoDep).connect()
            verify(mockIsoDep).timeout = 3000
            verify(mockEventSink).success("00")
        }
    }

    @Test
    fun `onTagFound emits IsoDepNotSupportedException when IsoDep is null`() {
        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(null)
            whenever(mockTag.id).thenReturn(byteArrayOf(0x00))
            whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
                val runnable = invocation.getArgument<Runnable>(0)
                runnable.run()
                null
            }

            tagReader.eventSink = mockEventSink
            tagReader.onTagFound(mockTag)

            verify(mockIsoDep, times(0)).connect()
            verify(mockIsoDep, times(0)).timeout = 3000
            verify(mockEventSink).error(IsoDepNotSupportedException())
        }
    }

    @Test
    fun `onTagFound emits TagConnectionException when connect throws`() {
        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(mockIsoDep)
            whenever(mockTag.id).thenReturn(byteArrayOf(0x00))
            whenever(mockIsoDep.connect()).thenThrow(IOException())
            whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
                val runnable = invocation.getArgument<Runnable>(0)
                runnable.run()
                null
            }

            tagReader.eventSink = mockEventSink
            tagReader.onTagFound(mockTag)

            verify(mockIsoDep).connect()
            verify(mockIsoDep, times(0)).timeout = 3000
            verify(mockEventSink).error(TagConnectionException())
        }
    }

    @Test
    fun `stopDiscovery throws NfcNotSupportedException when nfcAdapter is null`() {
        val tagReader = TagReader(activity = mockActivity, nfcAdapter = null)
        assertThrows<NfcNotSupportedException> { tagReader.stopDiscovery() }
    }

    @Test
    fun `stopDiscovery should not stop when it is already stopped`() {
        tagReader.isDiscoveryStarted = false
        tagReader.stopDiscovery()
        verify(mockNfcAdapter, times(0)).disableReaderMode(any())
    }

    @Test
    fun `stopDiscovery should stop when it is started`() {
        tagReader.isDiscoveryStarted = true
        tagReader.isoDep = mockIsoDep
        tagReader.stopDiscovery()

        assertNull(tagReader.isoDep)
        assertFalse(tagReader.isDiscoveryStarted)
        verify(mockNfcAdapter).disableReaderMode(mockActivity)
        verify(mockIsoDep).close()
    }

    @Test
    fun `sendCommand sends successfully`() {
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.transceive(byteArrayOf())).thenReturn(byteArrayOf(0x90.toByte(), 0x00))

        assertContentEquals(
            byteArrayOf(0x90.toByte(), 0x00),
            tagReader.sendCommand(byteArrayOf()),
        )
        verify(mockIsoDep).transceive(byteArrayOf())
    }

    @Test
    fun `sendCommand throws TagConnectionException if isoDep is null`() {
        tagReader.isoDep = null

        assertThrows<TagConnectionException> {
            tagReader.sendCommand(byteArrayOf())
        }
        verify(mockIsoDep, times(0)).transceive(byteArrayOf())
    }

    @Test
    fun `sendCommand throws TagConnectionException if TagLostException is thrown`() {
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.transceive(byteArrayOf())).thenThrow(TagLostException())

        assertThrows<TagConnectionException> {
            tagReader.sendCommand(byteArrayOf())
        }
        verify(mockIsoDep).transceive(byteArrayOf())
    }

    @Test
    fun `onListen should set eventSink correctly`() {
        tagReader.onListen(null, mockEventSink)

        assertEquals(mockEventSink, tagReader.eventSink)
    }

    @Test
    fun `onCancel should stop discovery and nullify eventSink`() {
        tagReader.eventSink = mockEventSink
        tagReader.isDiscoveryStarted = true
        tagReader.isoDep = mockIsoDep
        tagReader.onCancel(null)

        assertNull(tagReader.eventSink)
        assertNull(tagReader.isoDep)
        assertFalse(tagReader.isDiscoveryStarted)
        verify(mockNfcAdapter).disableReaderMode(mockActivity)
        verify(mockIsoDep).close()
    }
}
