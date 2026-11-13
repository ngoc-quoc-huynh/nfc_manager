package dev.huynh.nfc_manager_android.reader

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.IsoDepNotSupportedException
import dev.huynh.nfc_manager_android.NfcNotSupportedException
import dev.huynh.nfc_manager_android.TagConnectionException
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class TagReaderTest {
    private val mockActivity = mock<Activity>()
    private val mockEventSink = mock<EventSink>()
    private val mockIsoDep = mock<IsoDep>()
    private val mockNfcAdapter = mock<NfcAdapter>()
    private val mockTag = mock<Tag>()
    private val command = byteArrayOf(0x00, 0x00, 0x00, 0x00)
    private val ioExecutor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, IO_THREAD_NAME).apply { isDaemon = true }
        }
    private val mainThreadPosts = AtomicInteger()
    private var isoDepFor: (Tag) -> IsoDep? = { null }
    private lateinit var tagReader: TagReader

    @BeforeEach
    fun setup() {
        tagReader = newTagReader(mockNfcAdapter)
    }

    @AfterEach
    fun tearDown() {
        tagReader.dispose()
    }

    @Test
    fun `sendCommand sends successfully`() {
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.transceive(command)).thenReturn(byteArrayOf(0x90.toByte(), 0x00))

        assertContentEquals(
            byteArrayOf(0x90.toByte(), 0x00),
            sendCommand(command),
        )
        verify(mockIsoDep).transceive(command)
        assertEquals(1, mainThreadPosts.get())
    }

    @Test
    fun `default seams run tag I-O on the nfc-tag-io worker and reply on the UI thread`() {
        val lengthReadThread = AtomicReference<String>()
        whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
        }
        whenever(mockIsoDep.maxTransceiveLength).thenAnswer {
            lengthReadThread.set(Thread.currentThread().name)
            0
        }
        whenever(mockIsoDep.transceive(command)).thenReturn(byteArrayOf(0x90.toByte(), 0x00))
        val defaultTagReader = TagReader(activity = mockActivity, nfcAdapter = mockNfcAdapter)

        try {
            defaultTagReader.isoDep = mockIsoDep

            assertContentEquals(
                byteArrayOf(0x90.toByte(), 0x00),
                sendCommand(command, defaultTagReader),
            )
            assertEquals("nfc-tag-io", lengthReadThread.get())
            verify(mockActivity).runOnUiThread(any())
        } finally {
            defaultTagReader.dispose()
        }
    }

    @Test
    fun `sendCommand throws TagConnectionException if isoDep is null`() {
        tagReader.isoDep = null

        assertThrows<TagConnectionException> {
            sendCommand(command)
        }
        verify(mockIsoDep, times(0)).transceive(command)
    }

    @Test
    fun `sendCommand throws TagConnectionException if TagLostException is thrown`() {
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.transceive(command)).thenThrow(TagLostException())

        assertThrows<TagConnectionException> {
            sendCommand(command)
        }
        assertNull(tagReader.isoDep)
        verify(mockIsoDep).transceive(command)
        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
    }

    @Test
    fun `sendCommand throws TagConnectionException if SecurityException is thrown`() {
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.transceive(command)).thenThrow(SecurityException())

        assertThrows<TagConnectionException> {
            sendCommand(command)
        }
        assertNull(tagReader.isoDep)
        verify(mockIsoDep).transceive(command)
        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
    }

    @Test
    fun `sendCommand closes and clears IsoDep when IOException is thrown`() {
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.transceive(command)).thenThrow(IOException())

        assertThrows<TagConnectionException> { sendCommand(command) }

        assertNull(tagReader.isoDep)
        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
    }

    @Test
    fun `sendCommand maps IllegalStateException after cancellation`() {
        val transceiveStarted = CountDownLatch(1)
        val closeCalled = CountDownLatch(1)
        whenever(mockIsoDep.transceive(command)).thenAnswer {
            transceiveStarted.countDown()
            assertTrue(closeCalled.await(2, TimeUnit.SECONDS))
            throw IllegalStateException("connection closed")
        }
        whenever(mockIsoDep.close()).thenAnswer {
            closeCalled.countDown()
        }
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        tagReader.isoDep = mockIsoDep

        val caller = Executors.newSingleThreadExecutor()
        try {
            val commandResult = caller.submit<TagConnectionException> {
                assertThrows<TagConnectionException> { sendCommand(command) }
            }

            assertTrue(transceiveStarted.await(2, TimeUnit.SECONDS))
            tagReader.onCancel(null)
            commandResult.get(2, TimeUnit.SECONDS)
            assertNull(tagReader.isoDep)
        } finally {
            caller.shutdownNow()
        }
    }

    @Test
    fun `sendCommand preserves IllegalStateException while connection remains active`() {
        val failure = IllegalStateException("unexpected active connection state")
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.transceive(command)).thenThrow(failure)

        assertSame(
            failure,
            assertThrows<IllegalStateException> { sendCommand(command) },
        )

        verify(mockIsoDep, times(0)).close()
    }

    @Test
    fun `sendCommand rejects malformed APDU before transceive`() {
        tagReader.isoDep = mockIsoDep

        assertThrows<InvalidApduCommandException> {
            tagReader.sendCommand(byteArrayOf(), onSuccess = {}, onError = {})
        }

        verifyNoInteractions(mockIsoDep)
    }

    @Test
    fun `sendCommand rejects a command longer than maxTransceiveLength and keeps the tag connected`() {
        val oversizedCommand = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00)
        val lengthReadThread = AtomicReference<String>()
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.maxTransceiveLength).thenAnswer {
            lengthReadThread.set(Thread.currentThread().name)
            command.size
        }
        // Android rejects an oversized command with an IOException, which looks like a lost tag.
        whenever(mockIsoDep.transceive(oversizedCommand))
            .thenThrow(IOException("Transceive length exceeds supported maximum"))
        whenever(mockIsoDep.transceive(command)).thenReturn(byteArrayOf(0x90.toByte(), 0x00))

        assertThrows<InvalidApduCommandException> { sendCommand(oversizedCommand) }

        assertEquals(IO_THREAD_NAME, lengthReadThread.get())
        verify(mockIsoDep, never()).transceive(oversizedCommand)
        verify(mockIsoDep, never()).close()
        assertSame(mockIsoDep, tagReader.isoDep)
        assertContentEquals(byteArrayOf(0x90.toByte(), 0x00), sendCommand(command))
    }

    @Test
    fun `sendCommand skips the length check when maxTransceiveLength is unknown`() {
        // A 261-byte short APDU: Lc = FF, 255 data bytes, and Le.
        val longCommand = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0xFF.toByte()) + ByteArray(256)
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.maxTransceiveLength).thenReturn(0)
        whenever(mockIsoDep.transceive(longCommand)).thenReturn(byteArrayOf(0x90.toByte(), 0x00))

        assertContentEquals(byteArrayOf(0x90.toByte(), 0x00), sendCommand(longCommand))
        verify(mockIsoDep).transceive(longCommand)
    }

    @Test
    fun `sendCommand treats a SecurityException from maxTransceiveLength as a lost tag`() {
        tagReader.isoDep = mockIsoDep
        // Android throws SecurityException for a tag that is out of date after it left the field.
        whenever(mockIsoDep.maxTransceiveLength).thenThrow(SecurityException())

        assertThrows<TagConnectionException> { sendCommand(command) }

        assertNull(tagReader.isoDep)
        verify(mockIsoDep, never()).transceive(any())
        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
    }

    @Test
    fun `onListen emits NfcNotSupportedException if NfcAdapter is null`() {
        val tagReader = newTagReader(nfcAdapter = null)
        tagReader.onListen(null, mockEventSink)

        verify(mockEventSink).error(NfcNotSupportedException())
    }

    @Test
    fun `onListen starts discovery`() {
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)

        verify(mockNfcAdapter).enableReaderMode(
            eq(mockActivity),
            any(),
            eq(
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            ),
            isNull(),
        )
        verify(mockNfcAdapter, times(0)).disableReaderMode(mockActivity)
    }

    @Test
    fun `onListen emits INVALID_ARGUMENTS for malformed arguments without enabling reader mode`() {
        tagReader.onListen("invalid", mockEventSink)

        verifyInvalidArgumentsEmitted()
        verifyNoInteractions(mockNfcAdapter)
    }

    @Test
    fun `onListen emits INVALID_ARGUMENTS for a non-integer timeout without enabling reader mode`() {
        tagReader.onListen(mapOf("timeout" to "100"), mockEventSink)

        verifyInvalidArgumentsEmitted()
        verifyNoInteractions(mockNfcAdapter)
    }

    @Test
    fun `onListen emits INVALID_ARGUMENTS for a non-positive timeout without enabling reader mode`() {
        tagReader.onListen(mapOf("timeout" to 0), mockEventSink)

        verifyInvalidArgumentsEmitted()
        verifyNoInteractions(mockNfcAdapter)
    }

    @Test
    fun `onListen emits INVALID_ARGUMENTS for a negative timeout without enabling reader mode`() {
        tagReader.onListen(mapOf("timeout" to -1), mockEventSink)

        verifyInvalidArgumentsEmitted()
        verifyNoInteractions(mockNfcAdapter)
    }

    @Test
    fun `onListen emits INVALID_ARGUMENTS for a timeout beyond the Int range without enabling reader mode`() {
        // The standard message codec delivers a Dart int outside the 32-bit range as a Long.
        tagReader.onListen(mapOf("timeout" to Int.MAX_VALUE.toLong() + 1), mockEventSink)

        verifyInvalidArgumentsEmitted()
        verifyNoInteractions(mockNfcAdapter)
    }

    @Test
    fun `onListen emits NFC_ERROR and releases the session when enableReaderMode fails`() {
        doThrow(IllegalStateException("raw native detail"))
            .whenever(mockNfcAdapter)
            .enableReaderMode(any(), any(), any(), isNull())

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)

        verify(mockEventSink).error("NFC_ERROR", "Unexpected NFC reader error.", null)
        verifyNoMoreInteractions(mockEventSink)
        verify(mockNfcAdapter).disableReaderMode(mockActivity)
        assertNull(tagReader.eventSink)
    }

    @Test
    fun `onListen emits NFC_ERROR once when enabling and releasing reader mode both fail`() {
        doThrow(UnsupportedOperationException())
            .whenever(mockNfcAdapter)
            .enableReaderMode(any(), any(), any(), isNull())
        doThrow(UnsupportedOperationException())
            .whenever(mockNfcAdapter)
            .disableReaderMode(mockActivity)

        assertDoesNotThrow { tagReader.onListen(mapOf("timeout" to null), mockEventSink) }

        verify(mockEventSink, times(1)).error("NFC_ERROR", "Unexpected NFC reader error.", null)
        verifyNoMoreInteractions(mockEventSink)
        assertNull(tagReader.eventSink)
    }

    @Test
    fun `onTagFound emits success when IsoDep is available`() {
        isoDepFor = mapOf(mockTag to mockIsoDep)::get
        whenever(mockTag.id).thenReturn(byteArrayOf(0x00))

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)

        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).connect()
        verify(mockIsoDep, times(0)).timeout
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("00")
    }

    @Test
    fun `onTagFound emits success with timeout when IsoDep is available`() {
        isoDepFor = mapOf(mockTag to mockIsoDep)::get
        whenever(mockTag.id).thenReturn(byteArrayOf(0x00))

        tagReader.onListen(mapOf("timeout" to 3000), mockEventSink)
        discoverTag(mockTag)

        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("00")
        verify(mockIsoDep).timeout = 3000
    }

    @Test
    fun `onTagFound emits IsoDepNotSupportedException when IsoDep is null`() {
        isoDepFor = { null }
        whenever(mockTag.id).thenReturn(byteArrayOf(0x00))

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)

        verify(mockIsoDep, times(0)).connect()
        verify(mockIsoDep, times(0)).timeout = 3000
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).error(IsoDepNotSupportedException())
    }

    @Test
    fun `onTagFound emits TagConnectionException when IsoDep get throws SecurityException and keeps discovering`() {
        val secondTag = mock<Tag>()
        whenever(secondTag.id).thenReturn(byteArrayOf(0x02))
        isoDepFor = { tag ->
            // Android throws SecurityException for a tag that is out of date after it left the field.
            if (tag === mockTag) throw SecurityException()
            mockIsoDep
        }

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        assertDoesNotThrow { discoverTag(mockTag) }
        verify(mockEventSink).error(TagConnectionException())

        discoverTag(secondTag)

        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).connect()
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("02")
        verify(mockNfcAdapter, never()).disableReaderMode(mockActivity)
    }

    @Test
    fun `onTagFound emits TagConnectionException when connect throws`() {
        isoDepFor = mapOf(mockTag to mockIsoDep)::get
        whenever(mockTag.id).thenReturn(byteArrayOf(0x00))
        whenever(mockIsoDep.connect()).thenThrow(IOException())

        tagReader.onListen(mapOf("timeout" to 3000), mockEventSink)
        discoverTag(mockTag)

        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).connect()
        verify(mockIsoDep, times(0)).timeout = 3000
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).error(TagConnectionException())
    }

    @Test
    fun `onTagFound emits TagConnectionException when connect throws SecurityException`() {
        isoDepFor = mapOf(mockTag to mockIsoDep)::get
        whenever(mockIsoDep.connect()).thenThrow(SecurityException())

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)

        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).error(TagConnectionException())
    }

    @Test
    fun `onTagFound reports unexpected connection failures`() {
        isoDepFor = mapOf(mockTag to mockIsoDep)::get
        whenever(mockIsoDep.connect()).thenThrow(IllegalStateException())

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)

        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).error(
            "NFC_ERROR",
            "Unexpected NFC reader error.",
            null,
        )
    }

    @Test
    fun `onTagFound closes failed connection and allows a later tag`() {
        val secondTag = mock<Tag>()
        val secondIsoDep = mock<IsoDep>()
        whenever(mockTag.id).thenReturn(byteArrayOf(0x01))
        whenever(secondTag.id).thenReturn(byteArrayOf(0x02))
        isoDepFor = mapOf(mockTag to mockIsoDep, secondTag to secondIsoDep)::get
        whenever(mockIsoDep.connect()).thenThrow(IOException())

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).error(TagConnectionException())
        discoverTag(secondTag)

        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
        Mockito.verify(secondIsoDep, Mockito.timeout(2_000)).connect()
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("02")
    }

    @Test
    fun `onTagFound replaces the held tag with a newly discovered one`() {
        val secondTag = mock<Tag>()
        val secondIsoDep = mock<IsoDep>()
        whenever(mockTag.id).thenReturn(byteArrayOf(0x01))
        whenever(secondTag.id).thenReturn(byteArrayOf(0x02))
        isoDepFor = mapOf(mockTag to mockIsoDep, secondTag to secondIsoDep)::get

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("01")
        discoverTag(secondTag)
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("02")

        verify(mockIsoDep, Mockito.timeout(2_000)).close()
        verify(secondIsoDep).connect()
    }

    @Test
    fun `onTagFound skips queued connection work after cancellation`() {
        val executorStarted = CountDownLatch(1)
        val allowQueuedWork = CountDownLatch(1)
        ioExecutor.execute {
            executorStarted.countDown()
            assertTrue(allowQueuedWork.await(2, TimeUnit.SECONDS))
        }
        assertTrue(executorStarted.await(2, TimeUnit.SECONDS))
        isoDepFor = mapOf(mockTag to mockIsoDep)::get

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)
        tagReader.onCancel(null)

        val queuedWorkFinished = ioExecutor.submit {}
        allowQueuedWork.countDown()
        queuedWorkFinished.get(2, TimeUnit.SECONDS)

        verify(mockIsoDep, times(0)).connect()
    }

    @Test
    fun `onTagFound closes a tag connected after cancellation without holding the state lock`() {
        val connectStarted = CountDownLatch(1)
        val allowConnect = CountDownLatch(1)
        val closeBlockedOtherCallers = CompletableFuture<Boolean>()
        whenever(mockTag.id).thenReturn(byteArrayOf(0x01))
        whenever(mockIsoDep.connect()).thenAnswer {
            connectStarted.countDown()
            assertTrue(allowConnect.await(2, TimeUnit.SECONDS))
            null
        }
        whenever(mockIsoDep.close()).thenAnswer {
            // onCancel takes the state lock, so it finishes during close() only if the lock is free.
            val otherCaller = Thread { tagReader.onCancel(null) }.apply { isDaemon = true }
            otherCaller.start()
            otherCaller.join(1_000)
            closeBlockedOtherCallers.complete(otherCaller.isAlive)
            null
        }
        isoDepFor = mapOf(mockTag to mockIsoDep)::get

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)
        assertTrue(connectStarted.await(2, TimeUnit.SECONDS))
        tagReader.onCancel(null)
        allowConnect.countDown()

        assertEquals(false, closeBlockedOtherCallers.get(2, TimeUnit.SECONDS))
        verify(mockIsoDep, times(1)).close()
        verify(mockEventSink, never()).success(any())
        assertNull(tagReader.isoDep)
    }

    @Test
    fun `queued command from old session is rejected and old tag is closed only once`() {
        val firstCommand = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val queuedCommand = byteArrayOf(0x00, 0x01, 0x00, 0x00)
        val secondTag = mock<Tag>()
        val secondIsoDep = mock<IsoDep>()
        val transceiveStarted = CountDownLatch(1)
        val firstTagClosed = CountDownLatch(1)
        val firstCommandFinished = CountDownLatch(1)
        val queuedCommandFinished = CountDownLatch(1)
        val firstCommandError = AtomicReference<Throwable?>()
        val queuedCommandError = AtomicReference<Throwable?>()
        whenever(mockTag.id).thenReturn(byteArrayOf(0x01))
        whenever(secondTag.id).thenReturn(byteArrayOf(0x02))
        whenever(mockIsoDep.transceive(firstCommand)).thenAnswer {
            transceiveStarted.countDown()
            assertTrue(firstTagClosed.await(2, TimeUnit.SECONDS))
            throw IOException("session cancelled")
        }
        whenever(mockIsoDep.close()).thenAnswer {
            firstTagClosed.countDown()
        }
        isoDepFor = mapOf(mockTag to mockIsoDep, secondTag to secondIsoDep)::get

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("01")

        tagReader.sendCommand(
            firstCommand,
            onSuccess = {},
            onError = {
                firstCommandError.set(it)
                firstCommandFinished.countDown()
            },
        )
        assertTrue(transceiveStarted.await(2, TimeUnit.SECONDS))
        tagReader.sendCommand(
            queuedCommand,
            onSuccess = {},
            onError = {
                queuedCommandError.set(it)
                queuedCommandFinished.countDown()
            },
        )

        tagReader.onListen(mapOf("timeout" to 3000), mockEventSink)
        discoverTag(secondTag)

        assertTrue(firstCommandFinished.await(2, TimeUnit.SECONDS))
        assertTrue(queuedCommandFinished.await(2, TimeUnit.SECONDS))
        assertTrue(firstCommandError.get() is TagConnectionException)
        assertTrue(queuedCommandError.get() is TagConnectionException)
        Mockito.verify(secondIsoDep, Mockito.timeout(2_000)).connect()
        verify(secondIsoDep).timeout = 3000
        verify(secondIsoDep, times(0)).transceive(queuedCommand)
        verify(mockIsoDep, times(1)).close()
    }

    @Test
    fun `onListen releases the previous tag and accepts a tag in the new session`() {
        val secondTag = mock<Tag>()
        val secondIsoDep = mock<IsoDep>()
        whenever(mockTag.id).thenReturn(byteArrayOf(0x01))
        whenever(secondTag.id).thenReturn(byteArrayOf(0x02))
        isoDepFor = mapOf(mockTag to mockIsoDep, secondTag to secondIsoDep)::get

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("01")

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(secondTag)

        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
        Mockito.verify(secondIsoDep, Mockito.timeout(2_000)).connect()
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("02")
        verify(mockNfcAdapter).disableReaderMode(mockActivity)
    }

    @Test
    fun `onTagFound ignores a callback after cancellation`() {
        isoDepFor = mapOf(mockTag to mockIsoDep)::get
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        val callbackCaptor = argumentCaptor<NfcAdapter.ReaderCallback>()
        verify(mockNfcAdapter).enableReaderMode(
            eq(mockActivity),
            callbackCaptor.capture(),
            any(),
            isNull(),
        )
        tagReader.onCancel(null)
        callbackCaptor.firstValue.onTagDiscovered(mockTag)

        verifyNoInteractions(mockEventSink)
        verify(mockIsoDep, times(0)).connect()
    }

    @Test
    fun `onCancel clears state when IsoDep close throws IOException`() {
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.close()).thenThrow(IOException())

        tagReader.onCancel(null)

        assertNull(tagReader.eventSink)
        assertNull(tagReader.isoDep)
    }

    @Test
    fun `onCancel closes IsoDep to interrupt a blocked command`() {
        val transceiveStarted = CountDownLatch(1)
        val closeCalled = CountDownLatch(1)
        whenever(mockIsoDep.transceive(command)).thenAnswer {
            transceiveStarted.countDown()
            assertTrue(closeCalled.await(2, TimeUnit.SECONDS))
            throw IOException("closed")
        }
        whenever(mockIsoDep.close()).thenAnswer {
            closeCalled.countDown()
            throw IOException("already lost")
        }
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        tagReader.isoDep = mockIsoDep

        val caller = Executors.newSingleThreadExecutor()
        try {
            val commandResult = caller.submit<TagConnectionException> {
                assertThrows<TagConnectionException> { sendCommand(command) }
            }

            assertTrue(transceiveStarted.await(2, TimeUnit.SECONDS))
            tagReader.onCancel(null)
            commandResult.get(2, TimeUnit.SECONDS)
            assertNull(tagReader.isoDep)
        } finally {
            caller.shutdownNow()
        }
    }

    @Test
    fun `onCancel should stop discovery and nullify eventSink`() {
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        tagReader.isoDep = mockIsoDep
        tagReader.onCancel(null)

        assertNull(tagReader.eventSink)
        assertNull(tagReader.isoDep)
        verify(mockNfcAdapter).disableReaderMode(mockActivity)
        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
    }

    @Test
    fun `onCancel does not end the stream that Dart cancelled`() {
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)

        tagReader.onCancel(null)

        verify(mockEventSink, never()).endOfStream()
    }

    @Test
    fun `dispose during discovery ends the stream and stops discovery`() {
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        tagReader.isoDep = mockIsoDep

        tagReader.dispose()

        verify(mockEventSink).endOfStream()
        verifyNoMoreInteractions(mockEventSink)
        verify(mockNfcAdapter).disableReaderMode(mockActivity)
        verify(mockIsoDep).close()
        assertNull(tagReader.eventSink)
        assertNull(tagReader.isoDep)
    }

    @Test
    fun `dispose ends the stream when releasing reader mode fails on a destroyed activity`() {
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        doThrow(IllegalStateException("activity is already destroyed"))
            .whenever(mockNfcAdapter)
            .disableReaderMode(mockActivity)

        assertDoesNotThrow { tagReader.dispose() }

        verify(mockEventSink).endOfStream()
        verifyNoMoreInteractions(mockEventSink)
        assertNull(tagReader.eventSink)
    }

    @Test
    fun `dispose ends the stream only once`() {
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)

        tagReader.dispose()
        tagReader.dispose()

        verify(mockEventSink, times(1)).endOfStream()
    }

    @Test
    fun `dispose without discovery has no stream to end`() {
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        tagReader.onCancel(null)

        tagReader.dispose()

        verifyNoInteractions(mockEventSink)
    }

    private fun newTagReader(nfcAdapter: NfcAdapter?) =
        TagReader(
            activity = mockActivity,
            nfcAdapter = nfcAdapter,
            ioExecutor = ioExecutor,
            mainThread = { reply ->
                mainThreadPosts.incrementAndGet()
                reply.run()
            },
            isoDepFor = { tag -> isoDepFor(tag) },
        )

    private fun sendCommand(
        command: ByteArray,
        reader: TagReader = tagReader,
    ): ByteArray {
        val result = CompletableFuture<Result<ByteArray>>()
        reader.sendCommand(
            command,
            onSuccess = { result.complete(Result.success(it)) },
            onError = { result.complete(Result.failure(it)) },
        )
        return result.get(2, TimeUnit.SECONDS).getOrThrow()
    }

    private fun verifyInvalidArgumentsEmitted() {
        verify(mockEventSink).error(
            "INVALID_ARGUMENTS",
            "The arguments are missing or have the wrong type.",
            null,
        )
        verifyNoMoreInteractions(mockEventSink)
    }

    private fun discoverTag(tag: Tag) {
        val callbackCaptor = argumentCaptor<NfcAdapter.ReaderCallback>()
        verify(mockNfcAdapter, Mockito.atLeastOnce()).enableReaderMode(
            eq(mockActivity),
            callbackCaptor.capture(),
            any(),
            isNull(),
        )
        callbackCaptor.lastValue.onTagDiscovered(tag)
    }

    private companion object {
        const val IO_THREAD_NAME = "test-nfc-io"
    }
}
