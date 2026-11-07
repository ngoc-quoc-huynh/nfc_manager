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
import dev.huynh.nfc_manager_android.TagReader
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.plugin.common.EventChannel.EventSink
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
    private lateinit var tagReader: TagReader

    @BeforeEach
    fun setup() {
        whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
        }
        tagReader =
            TagReader(
                activity = mockActivity,
                nfcAdapter = mockNfcAdapter,
            )
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
        verify(mockActivity).runOnUiThread(any())
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

        val ioExecutor = Executors.newSingleThreadExecutor()
        try {
            val commandResult = ioExecutor.submit<TagConnectionException> {
                assertThrows<TagConnectionException> { sendCommand(command) }
            }

            assertTrue(transceiveStarted.await(2, TimeUnit.SECONDS))
            tagReader.onCancel(null)
            commandResult.get(2, TimeUnit.SECONDS)
            assertNull(tagReader.isoDep)
        } finally {
            ioExecutor.shutdownNow()
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
    fun `onListen emits NfcNotSupportedException if NfcAdapter is null`() {
        val tagReader = TagReader(activity = mockActivity, nfcAdapter = null)
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
    fun `onListen rejects malformed arguments without enabling reader mode`() {
        assertThrows<IllegalArgumentException> {
            tagReader.onListen("invalid", mockEventSink)
        }

        verifyNoInteractions(mockNfcAdapter)
    }

    @Test
    fun `onListen rejects a non-positive timeout without enabling reader mode`() {
        assertThrows<IllegalArgumentException> {
            tagReader.onListen(mapOf("timeout" to 0), mockEventSink)
        }

        verifyNoInteractions(mockNfcAdapter)
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

            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            discoverTag(mockTag)

            Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).connect()
            verify(mockIsoDep, times(0)).timeout
            Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("00")
        }
    }

    @Test
    fun `onTagFound emits success with timeout when IsoDep is available`() {
        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(mockIsoDep)
            whenever(mockTag.id).thenReturn(byteArrayOf(0x00))
            whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
                val runnable = invocation.getArgument<Runnable>(0)
                runnable.run()
                null
            }

            tagReader.onListen(mapOf("timeout" to 3000), mockEventSink)
            discoverTag(mockTag)

            Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("00")
            verify(mockIsoDep).timeout = 3000
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

            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            discoverTag(mockTag)

            verify(mockIsoDep, times(0)).connect()
            verify(mockIsoDep, times(0)).timeout = 3000
            Mockito.verify(mockEventSink, Mockito.timeout(2_000)).error(IsoDepNotSupportedException())
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

            tagReader.onListen(mapOf("timeout" to 3000), mockEventSink)
            discoverTag(mockTag)

            Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).connect()
            verify(mockIsoDep, times(0)).timeout = 3000
            Mockito.verify(mockEventSink, Mockito.timeout(2_000)).error(TagConnectionException())
        }
    }

    @Test
    fun `onTagFound emits TagConnectionException when connect throws SecurityException`() {
        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(mockIsoDep)
            whenever(mockIsoDep.connect()).thenThrow(SecurityException())
            whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
                invocation.getArgument<Runnable>(0).run()
                null
            }

            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            discoverTag(mockTag)
        }

        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).error(TagConnectionException())
    }

    @Test
    fun `onTagFound reports unexpected connection failures`() {
        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(mockIsoDep)
            whenever(mockIsoDep.connect()).thenThrow(IllegalStateException())
            whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
                invocation.getArgument<Runnable>(0).run()
                null
            }

            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            discoverTag(mockTag)
        }

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
        whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
            null
        }

        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(mockIsoDep)
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(secondTag) }.thenReturn(secondIsoDep)
            whenever(mockIsoDep.connect()).thenThrow(IOException())
            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            discoverTag(mockTag)
            Mockito.verify(mockEventSink, Mockito.timeout(2_000)).error(TagConnectionException())
            discoverTag(secondTag)
        }

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

        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(mockIsoDep)
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(secondTag) }.thenReturn(secondIsoDep)
            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            discoverTag(mockTag)
            Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("01")
            discoverTag(secondTag)
            Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("02")
        }

        verify(mockIsoDep, Mockito.timeout(2_000)).close()
        verify(secondIsoDep).connect()
    }

fun `onTagFound skips queued connection work after cancellation`() {
        val executor = ioExecutor()
        val executorStarted = CountDownLatch(1)
        val allowQueuedWork = CountDownLatch(1)
        executor.execute {
            executorStarted.countDown()
            assertTrue(allowQueuedWork.await(2, TimeUnit.SECONDS))
        }
        assertTrue(executorStarted.await(2, TimeUnit.SECONDS))

        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(mockIsoDep)
            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            discoverTag(mockTag)
            tagReader.onCancel(null)
        }

        val queuedWorkFinished = executor.submit {}
        allowQueuedWork.countDown()
        queuedWorkFinished.get(2, TimeUnit.SECONDS)

        verify(mockIsoDep, times(0)).connect()
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
        whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
            null
        }
        whenever(mockIsoDep.transceive(firstCommand)).thenAnswer {
            transceiveStarted.countDown()
            assertTrue(firstTagClosed.await(2, TimeUnit.SECONDS))
            throw IOException("session cancelled")
        }
        whenever(mockIsoDep.close()).thenAnswer {
            firstTagClosed.countDown()
        }

        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(mockIsoDep)
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(secondTag) }.thenReturn(secondIsoDep)
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
        }

        assertTrue(firstCommandFinished.await(2, TimeUnit.SECONDS))
        assertTrue(queuedCommandFinished.await(2, TimeUnit.SECONDS))
        assertTrue(firstCommandError.get() is TagConnectionException)
        assertTrue(queuedCommandError.get() is TagConnectionException)
        Mockito.verify(secondIsoDep, Mockito.timeout(2_000)).connect()
        verify(secondIsoDep).timeout = 3000
        verify(secondIsoDep, times(0)).transceive(queuedCommand)
        verify(mockIsoDep, times(1)).close()
    }

fun `onListen releases the previous tag and accepts a tag in the new session`() {
        val secondTag = mock<Tag>()
        val secondIsoDep = mock<IsoDep>()
        whenever(mockTag.id).thenReturn(byteArrayOf(0x01))
        whenever(secondTag.id).thenReturn(byteArrayOf(0x02))
        whenever(mockActivity.runOnUiThread(any())).thenAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
            null
        }

        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(mockTag) }.thenReturn(mockIsoDep)
            mockStaticIsoDep.`when`<IsoDep> { IsoDep.get(secondTag) }.thenReturn(secondIsoDep)

            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            discoverTag(mockTag)
            Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("01")

            tagReader.onListen(mapOf("timeout" to null), mockEventSink)
            discoverTag(secondTag)
        }

        Mockito.verify(mockIsoDep, Mockito.timeout(2_000)).close()
        Mockito.verify(secondIsoDep, Mockito.timeout(2_000)).connect()
        Mockito.verify(mockEventSink, Mockito.timeout(2_000)).success("02")
        verify(mockNfcAdapter).disableReaderMode(mockActivity)
    }

    @Test
    fun `onTagFound ignores a callback after cancellation`() {
        Mockito.mockStatic(IsoDep::class.java).use { mockStaticIsoDep ->
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

        val ioExecutor = Executors.newSingleThreadExecutor()
        try {
            val commandResult = ioExecutor.submit<TagConnectionException> {
                assertThrows<TagConnectionException> { sendCommand(command) }
            }

            assertTrue(transceiveStarted.await(2, TimeUnit.SECONDS))
            tagReader.onCancel(null)
            commandResult.get(2, TimeUnit.SECONDS)
            assertNull(tagReader.isoDep)
        } finally {
            ioExecutor.shutdownNow()
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

    private fun sendCommand(command: ByteArray): ByteArray {
        val result = CompletableFuture<Result<ByteArray>>()
        tagReader.sendCommand(
            command,
            onSuccess = { result.complete(Result.success(it)) },
            onError = { result.complete(Result.failure(it)) },
        )
        return result.get(2, TimeUnit.SECONDS).getOrThrow()
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

    private fun ioExecutor(): ExecutorService =
        TagReader::class.java.getDeclaredField("ioExecutor").apply {
            isAccessible = true
        }.get(tagReader) as ExecutorService
}
