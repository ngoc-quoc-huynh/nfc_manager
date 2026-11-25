package dev.huynh.nfc_manager_android.reader

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.InvalidArgumentsException
import dev.huynh.nfc_manager_android.IsoDepNotSupportedException
import dev.huynh.nfc_manager_android.NfcException
import dev.huynh.nfc_manager_android.NfcNotSupportedException
import dev.huynh.nfc_manager_android.TagConnectionException
import dev.huynh.nfc_manager_android.UnexpectedNfcException
import dev.huynh.nfc_manager_android.error
import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TagReaderTest {
    private val mockActivity = mock<Activity>()
    private val mockEventSink = mock<EventSink>()
    private val mockIsoDep = mock<IsoDep>()
    private val mockNfcAdapter = mock<NfcAdapter>()
    private val mockTag = mock<Tag>()
    private val command = byteArrayOf(0x00, 0x00, 0x00, 0x00)
    private val ok = byteArrayOf(0x90.toByte(), 0x00)
    private val ioExecutor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, IO_THREAD_NAME).apply { isDaemon = true }
        }
    private val mainThreadPosts = AtomicInteger()
    private var isoDepFor: (Tag) -> IsoDep? = { null }
    private lateinit var tagReader: TagReader

    @BeforeEach
    fun setup() {
        tagReader = newTagReader()
    }

    @AfterEach
    fun tearDown() {
        tagReader.dispose()
    }

    @Test
    fun `sendCommand returns the tag response on the main thread`() {
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.transceive(command)).thenReturn(ok)

        assertContentEquals(
            ok,
            sendCommand(command),
        )
        verify(mockIsoDep).transceive(command)
        assertEquals(1, mainThreadPosts.get())
    }

    @Test
    fun `sendCommand leaves the tag connected when an inline success callback throws`() {
        val worker = mock<ExecutorService>()
        val task = argumentCaptor<Runnable>()
        val callbackError = IOException("Callback failed")
        val onError = mock<(NfcException) -> Unit>()
        tagReader = newTagReader(ioExecutor = worker, mainThreadExecutor = Runnable::run)
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.transceive(command)).thenReturn(ok)

        tagReader.sendCommand(command, onSuccess = { throw callbackError }, onError = onError)
        verify(worker).execute(task.capture())

        assertSame(callbackError, assertThrows<IOException> { task.firstValue.run() })
        verifyNoInteractions(onError)
        verify(mockIsoDep, never()).close()
        assertSame(mockIsoDep, tagReader.isoDep)
    }

    @Test
    fun `sendCommand replies through Activity runOnUiThread by default`() {
        val lengthReadThread = AtomicReference<String>()
        whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
        }
        whenever(mockIsoDep.maxTransceiveLength).thenAnswer {
            lengthReadThread.set(Thread.currentThread().name)
            0
        }
        whenever(mockIsoDep.transceive(command)).thenReturn(ok)
        val defaultTagReader = TagReader(activity = mockActivity, nfcAdapter = mockNfcAdapter)

        try {
            defaultTagReader.isoDep = mockIsoDep

            assertContentEquals(
                ok,
                sendCommand(command, defaultTagReader),
            )
            assertEquals("nfc-tag-io", lengthReadThread.get())
            verify(mockActivity).runOnUiThread(any())
        } finally {
            defaultTagReader.dispose()
        }
    }

    @Test
    fun `sendCommand reports TagConnectionException without a connected tag`() {
        tagReader.isoDep = null

        assertThrows<TagConnectionException> {
            sendCommand(command)
        }
        verify(mockIsoDep, never()).transceive(command)
    }

    @Test
    fun `sendCommand closes the tag and throws TagConnectionException when transceive fails`() =
        listOf(TagLostException(), SecurityException(), IOException()).forEach { failure ->
            val isoDep = mock<IsoDep>()
            tagReader.isoDep = isoDep
            whenever(isoDep.transceive(command)).thenThrow(failure)

            assertThrows<TagConnectionException> { sendCommand(command) }

            assertNull(tagReader.isoDep)
            verify(isoDep, timeout(TIMEOUT_MS)).close()
        }

    @Test
    fun `sendCommand reports an unexpected transceive failure while the tag stays connected`() {
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.transceive(command)).thenThrow(IllegalStateException("raw native detail"))

        assertThrows<UnexpectedNfcException> { sendCommand(command) }

        verify(mockIsoDep, never()).close()
        assertSame(mockIsoDep, tagReader.isoDep)
    }

    @Test
    fun `sendCommand rejects malformed commands before transceive`() {
        tagReader.isoDep = mockIsoDep
        listOf(
            byteArrayOf(),
            // Lc says 5 data bytes, but only 1 follows.
            byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x05, 0x01),
            // Lc 00 cannot be followed by data.
            byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x00, 0x01),
        ).forEach { malformed ->
            assertThrows<InvalidApduCommandException> { sendCommand(malformed) }
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
        whenever(mockIsoDep.transceive(command)).thenReturn(ok)

        assertThrows<InvalidApduCommandException> { sendCommand(oversizedCommand) }

        assertEquals(IO_THREAD_NAME, lengthReadThread.get())
        verify(mockIsoDep, never()).transceive(oversizedCommand)
        verify(mockIsoDep, never()).close()
        assertSame(mockIsoDep, tagReader.isoDep)
        assertContentEquals(ok, sendCommand(command))
    }

    @Test
    fun `sendCommand skips the length check when maxTransceiveLength is unknown`() {
        // A 261-byte short APDU: Lc = FF, 255 data bytes, and Le.
        val longCommand = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0xFF.toByte()) + ByteArray(256)
        tagReader.isoDep = mockIsoDep
        whenever(mockIsoDep.maxTransceiveLength).thenReturn(0)
        whenever(mockIsoDep.transceive(longCommand)).thenReturn(ok)

        assertContentEquals(ok, sendCommand(longCommand))
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
        verify(mockIsoDep).close()
    }

    @Test
    fun `onListen emits NfcNotSupportedException if NfcAdapter is null`() {
        val tagReader = newTagReader(nfcAdapter = null)
        tagReader.onListen(null, mockEventSink)

        verify(mockEventSink).error(NfcNotSupportedException())
        verify(mockEventSink).endOfStream()
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
        verify(mockNfcAdapter, never()).disableReaderMode(mockActivity)
    }

    @Test
    fun `onListen emits INVALID_ARGUMENTS for invalid arguments without enabling reader mode`() {
        listOf(
            "invalid",
            mapOf("timeout" to "100"),
            mapOf("timeout" to 0),
            mapOf("timeout" to -1),
            // The standard message codec delivers a Dart int outside the 32-bit range as a Long.
            mapOf("timeout" to Int.MAX_VALUE.toLong() + 1),
            mapOf("timeout" to (1L shl 32) + 1),
        ).forEach { arguments ->
            val events = mock<EventSink>()
            tagReader.onListen(arguments, events)
            verifyInvalidArgumentsEmitted(events)
        }

        verifyNoInteractions(mockNfcAdapter)
    }

    @Test
    fun `onListen emits NFC_ERROR and releases the session when enableReaderMode fails`() {
        doThrow(IllegalStateException("raw native detail"))
            .whenever(mockNfcAdapter)
            .enableReaderMode(any(), any(), any(), isNull())

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)

        verify(mockEventSink).error(UnexpectedNfcException())
        verify(mockEventSink).endOfStream()
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

        verify(mockEventSink, times(1)).error(UnexpectedNfcException())
        verify(mockEventSink).endOfStream()
        verifyNoMoreInteractions(mockEventSink)
        assertNull(tagReader.eventSink)
    }

    @Test
    fun `onTagFound connects with the requested timeout and emits the tag ID`() =
        listOf(null, 3000).forEach { requestedTimeout ->
            val isoDep = mock<IsoDep>()
            val events = mock<EventSink>()
            isoDepFor = mapOf(mockTag to isoDep)::get
            whenever(mockTag.id).thenReturn(byteArrayOf(0x00))

            tagReader.onListen(mapOf("timeout" to requestedTimeout), events)
            discoverTag(mockTag)

            verify(events, timeout(TIMEOUT_MS)).success("00")
            verify(isoDep).connect()
            if (requestedTimeout == null) {
                verify(isoDep, never()).timeout = any()
            } else {
                verify(isoDep).timeout = requestedTimeout
            }
            tagReader.onCancel(null)
        }

    @Test
    fun `onTagFound emits IsoDepNotSupportedException when IsoDep is null`() {
        isoDepFor = { null }
        whenever(mockTag.id).thenReturn(byteArrayOf(0x00))

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)

        verify(mockIsoDep, never()).connect()
        verify(mockEventSink, timeout(TIMEOUT_MS)).error(IsoDepNotSupportedException())
    }

    @Test
    fun `onTagFound reports a stale tag and keeps discovering`() {
        val secondTag = mock<Tag>()
        whenever(secondTag.id).thenReturn(byteArrayOf(0x02))
        isoDepFor = { tag ->
            // Android throws SecurityException for a tag that is out of date after it left the field.
            if (tag === mockTag) throw SecurityException()
            mockIsoDep
        }
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        assertDoesNotThrow { discoverTag(mockTag) }
        verify(mockEventSink, times(1)).error(TagConnectionException())

        discoverTag(secondTag)
        ioExecutor.submit {}.get(2, TimeUnit.SECONDS)

        verify(mockIsoDep).connect()
        verify(mockEventSink, times(1)).success("02")
        verifyNoMoreInteractions(mockEventSink)
        verify(mockNfcAdapter, never()).disableReaderMode(mockActivity)
    }

    @Test
    fun `onTagFound closes the tag and reports a failed connect`() =
        listOf<Pair<Exception, NfcException>>(
            SecurityException() to TagConnectionException(),
            IllegalStateException() to UnexpectedNfcException(),
        ).forEach { (failure, error) ->
            val isoDep = mock<IsoDep>()
            val events = mock<EventSink>()
            isoDepFor = mapOf(mockTag to isoDep)::get
            whenever(isoDep.connect()).thenThrow(failure)

            tagReader.onListen(mapOf("timeout" to null), events)
            discoverTag(mockTag)

            verify(isoDep, timeout(TIMEOUT_MS)).close()
            verify(events, timeout(TIMEOUT_MS)).error(error)
            tagReader.onCancel(null)
        }

    @Test
    fun `onTagFound closes a failed connection and accepts the next tag`() {
        val secondTag = mock<Tag>()
        val secondIsoDep = mock<IsoDep>()
        whenever(mockTag.id).thenReturn(byteArrayOf(0x01))
        whenever(secondTag.id).thenReturn(byteArrayOf(0x02))
        isoDepFor = mapOf(mockTag to mockIsoDep, secondTag to secondIsoDep)::get
        whenever(mockIsoDep.connect()).thenThrow(IOException())

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)
        verify(mockEventSink, timeout(TIMEOUT_MS)).error(TagConnectionException())
        discoverTag(secondTag)

        verify(mockIsoDep, timeout(TIMEOUT_MS)).close()
        verify(secondIsoDep, timeout(TIMEOUT_MS)).connect()
        verify(mockEventSink, timeout(TIMEOUT_MS)).success("02")
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
        verify(mockEventSink, timeout(TIMEOUT_MS)).success("01")
        discoverTag(secondTag)
        verify(mockEventSink, timeout(TIMEOUT_MS)).success("02")

        verify(mockIsoDep, timeout(TIMEOUT_MS)).close()
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

        verify(mockIsoDep, never()).connect()
    }

    @Test
    fun `dispose drops a discovery event already queued for the main thread`() {
        val mainThreadCallbackQueued = CountDownLatch(1)
        val pendingMainThreadCallback = AtomicReference<Runnable?>()
        whenever(mockTag.id).thenReturn(byteArrayOf(0x00))
        isoDepFor = { mockIsoDep }
        tagReader =
            newTagReader(
                mainThreadExecutor = { callback ->
                    pendingMainThreadCallback.set(callback)
                    mainThreadCallbackQueued.countDown()
                },
            )

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        discoverTag(mockTag)

        assertTrue(mainThreadCallbackQueued.await(2, TimeUnit.SECONDS))
        tagReader.dispose()
        pendingMainThreadCallback.get()!!.run()

        verify(mockEventSink).endOfStream()
        verifyNoMoreInteractions(mockEventSink)
        verify(mockIsoDep).connect()
    }

    @Test
    fun `onTagFound closes a tag connected after cancellation without holding the state lock`() {
        val connectStarted = CountDownLatch(1)
        val allowConnect = CountDownLatch(1)
        val closeStarted = CountDownLatch(1)
        val allowClose = CountDownLatch(1)
        val contenderStarted = CountDownLatch(1)
        val closeContender = Executors.newSingleThreadExecutor()
        whenever(mockTag.id).thenReturn(byteArrayOf(0x01))
        whenever(mockIsoDep.connect()).thenAnswer {
            connectStarted.countDown()
            assertTrue(allowConnect.await(2, TimeUnit.SECONDS))
            null
        }
        whenever(mockIsoDep.close()).thenAnswer {
            closeStarted.countDown()
            allowClose.await()
            null
        }
        isoDepFor = mapOf(mockTag to mockIsoDep)::get

        try {
            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            discoverTag(mockTag)
            assertTrue(connectStarted.await(2, TimeUnit.SECONDS))

            val cancelContender =
                closeContender.submit {
                    contenderStarted.countDown()
                    assertTrue(closeStarted.await(2, TimeUnit.SECONDS))
                    tagReader.onCancel(null)
                }
            assertTrue(contenderStarted.await(2, TimeUnit.SECONDS))

            tagReader.onCancel(null)
            allowConnect.countDown()

            assertTrue(closeStarted.await(2, TimeUnit.SECONDS))
            cancelContender.get(2, TimeUnit.SECONDS)
            allowClose.countDown()

            val closeWorkFinished = ioExecutor.submit {}
            closeWorkFinished.get(2, TimeUnit.SECONDS)
            verify(mockIsoDep, times(1)).close()
            verify(mockEventSink, never()).success(any())
            assertNull(tagReader.isoDep)
        } finally {
            allowConnect.countDown()
            allowClose.countDown()
            closeContender.shutdownNow()
        }
    }

    @Test
    fun `onListen fails commands from the old session and closes the old tag once`() {
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
        verify(mockEventSink, timeout(TIMEOUT_MS)).success("01")

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
        verify(mockEventSink, timeout(TIMEOUT_MS)).success("02")

        assertTrue(firstCommandFinished.await(2, TimeUnit.SECONDS))
        assertTrue(queuedCommandFinished.await(2, TimeUnit.SECONDS))
        assertIs<TagConnectionException>(firstCommandError.get())
        assertIs<TagConnectionException>(queuedCommandError.get())
        verify(secondIsoDep, timeout(TIMEOUT_MS)).connect()
        verify(secondIsoDep).timeout = 3000
        verify(secondIsoDep, never()).transceive(queuedCommand)
        verify(mockIsoDep, times(1)).close()
        verify(mockNfcAdapter).disableReaderMode(mockActivity)
    }

    @Test
    fun `onTagFound ignores a callback after cancellation`() {
        isoDepFor = mapOf(mockTag to mockIsoDep)::get
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)

        tagReader.onCancel(null)
        discoverTag(mockTag)

        verifyNoInteractions(mockEventSink)
        verify(mockIsoDep, never()).connect()
    }

    @Test
    fun `onCancel closes IsoDep to interrupt a blocked command, which reports TagConnectionException`() =
        // An interrupted transceive fails with an IOException or, after close, an IllegalStateException.
        listOf(IOException("closed"), IllegalStateException("connection closed")).forEach { failure ->
            val isoDep = mock<IsoDep>()
            val transceiveStarted = CountDownLatch(1)
            val closeCalled = CountDownLatch(1)
            whenever(isoDep.transceive(command)).thenAnswer {
                transceiveStarted.countDown()
                assertTrue(closeCalled.await(2, TimeUnit.SECONDS))
                throw failure
            }
            whenever(isoDep.close()).thenAnswer {
                closeCalled.countDown()
                throw IOException("already lost")
            }
            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            tagReader.isoDep = isoDep

            val caller = Executors.newSingleThreadExecutor()
            try {
                val commandResult =
                    caller.submit<TagConnectionException> {
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
    fun `onCancel stops discovery and clears state even if closing the tag fails`() {
        listOf(false, true).forEach { closeFails ->
            val isoDep = mock<IsoDep>()
            if (closeFails) whenever(isoDep.close()).thenThrow(IOException())
            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            tagReader.isoDep = isoDep

            tagReader.onCancel(null)

            assertNull(tagReader.eventSink)
            assertNull(tagReader.isoDep)
            verify(isoDep, timeout(TIMEOUT_MS)).close()
        }

        verify(mockNfcAdapter, times(2)).disableReaderMode(mockActivity)
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
    fun `dispose is safe to repeat when disableReaderMode fails`() {
        tagReader.onListen(mapOf("timeout" to 3000), mockEventSink)
        tagReader.isoDep = mockIsoDep
        doThrow(IllegalStateException("activity is already destroyed"))
            .whenever(mockNfcAdapter)
            .disableReaderMode(mockActivity)

        assertDoesNotThrow { tagReader.dispose() }
        assertDoesNotThrow { tagReader.dispose() }

        verify(mockEventSink).endOfStream()
        verifyNoMoreInteractions(mockEventSink)
        verify(mockNfcAdapter, times(1)).disableReaderMode(mockActivity)
        verify(mockIsoDep, times(1)).close()
        assertNull(tagReader.eventSink)
        assertNull(tagReader.isoDep)
        assertNull(tagReader.timeout)
    }

    @Test
    fun `onListen after dispose does not start discovery`() {
        tagReader.dispose()

        tagReader.onListen(mapOf("timeout" to null), mockEventSink)

        verifyNoInteractions(mockEventSink)
        verify(mockNfcAdapter, never()).enableReaderMode(any(), any(), any(), isNull())
    }

    @Test
    fun `dispose after onCancel does not end the stream`() {
        tagReader.onListen(mapOf("timeout" to null), mockEventSink)
        tagReader.onCancel(null)

        tagReader.dispose()

        verifyNoInteractions(mockEventSink)
    }

    private fun newTagReader(
        nfcAdapter: NfcAdapter? = mockNfcAdapter,
        ioExecutor: ExecutorService = this.ioExecutor,
        mainThreadExecutor: Executor =
            Executor { reply ->
                mainThreadPosts.incrementAndGet()
                reply.run()
            },
    ) = TagReader(
        activity = mockActivity,
        nfcAdapter = nfcAdapter,
        ioExecutor = ioExecutor,
        mainThreadExecutor = mainThreadExecutor,
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

    private fun verifyInvalidArgumentsEmitted(events: EventSink = mockEventSink) {
        verify(events).error(InvalidArgumentsException())
        verify(events).endOfStream()
        verifyNoMoreInteractions(events)
    }

    private fun discoverTag(tag: Tag) {
        val callbackCaptor = argumentCaptor<NfcAdapter.ReaderCallback>()
        verify(mockNfcAdapter, atLeastOnce()).enableReaderMode(
            eq(mockActivity),
            callbackCaptor.capture(),
            any(),
            isNull(),
        )
        callbackCaptor.lastValue.onTagDiscovered(tag)
    }

    private companion object {
        const val IO_THREAD_NAME = "test-nfc-io"
        const val TIMEOUT_MS = 2_000L
    }
}
