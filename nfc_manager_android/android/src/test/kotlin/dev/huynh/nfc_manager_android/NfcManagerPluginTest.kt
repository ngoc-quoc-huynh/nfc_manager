package dev.huynh.nfc_manager_android

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.util.Log
import dev.huynh.nfc_manager_android.hce.HostCardEmulationService
import dev.huynh.nfc_manager_android.hce.HostCardEmulationSessionHolder
import dev.huynh.nfc_manager_android.hce.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.hce.HostCardEmulationStreamHandler
import dev.huynh.nfc_manager_android.hce.ResponseApdu
import dev.huynh.nfc_manager_android.reader.TagReader
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.FlutterException
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.StandardMethodCodec
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.nio.ByteBuffer
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class NfcManagerPluginTest {
    private val mockFeatureChecker = mock<FeatureChecker>()
    private val mockTagReader = mock<TagReader>()
    private val plugin = NfcManagerPlugin().apply { tagReader = mockTagReader }
    private val aid = byteArrayOf(0xF0.toByte(), 0x01, 0x02, 0x03, 0x04)
    private val hceArguments = mapOf("aid" to aid, "pin" to byteArrayOf(0x02))
    private val selectCommand = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid

    @AfterEach
    fun tearDown() {
        HostCardEmulationSessionHolder.active?.let { HostCardEmulationSessionHolder.clear(it.owner) }
    }

    @Test
    fun `feature checks reply with the FeatureChecker result`() {
        val channels = PluginChannels()
        // Attaching to the engine creates a real checker, so replace it after attaching.
        plugin.featureChecker = mockFeatureChecker
        listOf<Pair<String, FeatureChecker.() -> Boolean>>(
            "isHceSupported" to { isHceSupported() },
            "isNfcSupported" to { isNfcSupported() },
            "isNfcEnabled" to { isNfcEnabled() },
        ).forEach { (method, check) ->
            listOf(true, false).forEach { supported ->
                whenever(mockFeatureChecker.check()).thenReturn(supported)

                assertEquals(listOf(supported), channels.request(method))
            }
        }
    }

    @Test
    fun `sendCommand passes binary APDU bytes both ways`() {
        val channels = PluginChannels()
        val command = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x02, 0x00, 0xFF.toByte())
        val response = byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0xFF.toByte(), 0x90.toByte(), 0x00)
        whenever(mockTagReader.sendCommand(any(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<(ByteArray) -> Unit>(1)(response)
        }

        val reply = channels.request("sendCommand", command)

        val sent = argumentCaptor<ByteArray>()
        verify(mockTagReader).sendCommand(sent.capture(), any(), any())
        assertContentEquals(command, sent.firstValue)
        assertContentEquals(response, assertIs<ByteArray>(reply.single()))
    }

    @Test
    fun `sendCommand reports a reader error with its code and message only`() {
        val channels = PluginChannels()
        whenever(mockTagReader.sendCommand(any(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<(NfcException) -> Unit>(2)(InvalidApduCommandException())
        }

        assertEquals(
            listOf(
                "INVALID_APDU_COMMAND",
                "The APDU command is malformed or longer than Android's transceive limit.",
                null,
            ),
            channels.request("sendCommand", byteArrayOf()),
        )
    }

    @Test
    fun `sendCommand without an activity reports TAG_CONNECTION_FAILED`() {
        val channels = PluginChannels()
        plugin.tagReader = null

        assertEquals(
            listOf(
                "TAG_CONNECTION_FAILED",
                "Failed to establish a connection with the NFC tag or the tag was lost.",
                null,
            ),
            channels.request("sendCommand", byteArrayOf()),
        )
    }

    @Test
    fun `discovery reports an error and ends when started without an activity`() {
        val channels = PluginChannels()
        plugin.tagReader = null

        assertNull(methodCodec.decodeEnvelope(assertNotNull(channels.call(DISCOVERY_CHANNEL, "listen"))))

        val event = argumentCaptor<ByteBuffer>()
        verify(channels.messenger, times(2)).send(eq(DISCOVERY_CHANNEL), event.capture())
        val error = assertFailsWith<FlutterException> { methodCodec.decodeEnvelope(assertNotNull(event.firstValue).apply { flip() }) }
        assertEquals("NFC_ERROR", error.code)
        assertNull(event.lastValue)
    }

    @Test
    fun `discovery cancel stops the current reader`() {
        val channels = PluginChannels()
        channels.attachActivity()
        assertNotNull(channels.call(DISCOVERY_CHANNEL, "listen"))
        assertNotNull(assertNotNull(plugin.tagReader).eventSink)

        assertNull(methodCodec.decodeEnvelope(assertNotNull(channels.call(DISCOVERY_CHANNEL, "cancel"))))

        assertNull(assertNotNull(plugin.tagReader).eventSink)
        plugin.onDetachedFromActivity()
    }

    @Test
    fun `discovery accepts cancellation after an activity detaches`() {
        val channels = PluginChannels()
        channels.attachActivity()
        assertNull(methodCodec.decodeEnvelope(assertNotNull(channels.call(DISCOVERY_CHANNEL, "listen"))))

        plugin.onDetachedFromActivity()

        assertNull(methodCodec.decodeEnvelope(assertNotNull(channels.call(DISCOVERY_CHANNEL, "cancel"))))
    }

    @Test
    fun `discovery delivers events from the reader after activity reattachment`() {
        val channels = PluginChannels()
        channels.attachActivity()
        assertNotNull(channels.call(DISCOVERY_CHANNEL, "listen"))
        val previousReader = plugin.tagReader
        plugin.onDetachedFromActivityForConfigChanges()
        assertNotNull(channels.call(DISCOVERY_CHANNEL, "cancel"))

        channels.reattachActivityForConfigChanges()
        assertNotNull(channels.call(DISCOVERY_CHANNEL, "listen"))
        val reader = assertNotNull(plugin.tagReader)
        assertNull(previousReader?.eventSink)
        assertNotNull(reader.eventSink).success("04A1")

        val event = argumentCaptor<ByteBuffer>()
        verify(channels.messenger, times(2)).send(eq(DISCOVERY_CHANNEL), event.capture())
        assertNull(event.firstValue)
        assertEquals("04A1", methodCodec.decodeEnvelope(event.lastValue.apply { flip() }))
        plugin.onDetachedFromActivity()
    }

    @Test
    fun `activity detachment during discovery ends the discovery stream`() {
        val mockActivity = mock<Activity>()
        val mockNfcAdapter = mock<NfcAdapter>()
        val mockEventSink = mock<EventSink>()
        val tagReader = TagReader(activity = mockActivity, nfcAdapter = mockNfcAdapter)
        plugin.tagReader = tagReader
        tagReader.onListen(null, mockEventSink)

        plugin.onDetachedFromActivity()

        verify(mockEventSink).endOfStream()
        verifyNoMoreInteractions(mockEventSink)
        verify(mockNfcAdapter).disableReaderMode(mockActivity)
        assertNull(plugin.tagReader)
    }

    @Test
    fun `engine detachment clears the channel handlers and disposes the tag reader`() {
        val channels = PluginChannels()

        channels.detachEngine()

        (HOST_API_METHODS.map(::hostApiChannel) + listOf(DISCOVERY_CHANNEL, HCE_CHANNEL)).forEach { channel ->
            verify(channels.messenger).setMessageHandler(channel, null)
        }
        verify(mockTagReader).dispose()
    }

    @Test
    fun `engine detachment during emulation stops HCE responses`() {
        val channels = PluginChannels()
        val mockEventSink = mock<EventSink>()
        assertNotNull(plugin.hostCardEmulationStreamHandler).onListen(hceArguments, mockEventSink)

        channels.detachEngine()
        val response = HostCardEmulationService().processCommandApdu(selectCommand, null)

        assertContentEquals(ResponseApdu.HCE_NOT_READY.toByteArray(), response)
        assertNull(plugin.hostCardEmulationStreamHandler)
        verify(mockEventSink).success(HostCardEmulationStatus.READY.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `engine detachment keeps an HCE session started by another engine`() {
        val channels = PluginChannels()
        val mockEventSink = mock<EventSink>()
        HostCardEmulationStreamHandler().onListen(hceArguments, mockEventSink)

        channels.detachEngine()
        val response = HostCardEmulationService().processCommandApdu(selectCommand, null)

        assertContentEquals(ResponseApdu.OK.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.AID_SELECTED.name)
    }

    @Test
    fun `HCE rejects an invalid AID length and accepts the next listen`() {
        val channels = PluginChannels()

        assertNull(methodCodec.decodeEnvelope(assertNotNull(channels.call(HCE_CHANNEL, "listen", hceArguments + ("aid" to ByteArray(4))))))
        assertNull(HostCardEmulationSessionHolder.active)

        assertNull(methodCodec.decodeEnvelope(assertNotNull(channels.call(HCE_CHANNEL, "listen", hceArguments))))
        assertSame<EventChannel.StreamHandler?>(
            plugin.hostCardEmulationStreamHandler,
            assertNotNull(HostCardEmulationSessionHolder.active).owner,
        )

        val events = argumentCaptor<ByteBuffer>()
        verify(channels.messenger, times(3)).send(eq(HCE_CHANNEL), events.capture())
        val invalidEvent = assertNotNull(events.firstValue).apply { flip() }
        val invalidArguments = assertFailsWith<FlutterException> { methodCodec.decodeEnvelope(invalidEvent) }
        assertEquals("INVALID_ARGUMENTS", invalidArguments.code)
        assertEquals("HCE requires an AID of 5–16 bytes and a PIN of 1–255 bytes.", invalidArguments.message)
        assertNull(invalidArguments.details)
        assertNull(events.allValues[1])
        assertEquals("READY", methodCodec.decodeEnvelope(assertNotNull(events.lastValue).apply { flip() }))
    }

    @Test
    fun `a queued HCE listen cannot restore or replace a session after engine detachment`() {
        val channels = PluginChannels()
        // Flutter may still deliver a listen to the handler it looked up before detachment.
        val handler = channels.handler(HCE_CHANNEL)

        // The rejected listen is logged by Flutter's stream request handler.
        fun deliverListen() = mockStatic(Log::class.java).use { channels.send(handler, "listen", hceArguments) }

        val service = HostCardEmulationService()
        deliverListen()
        assertContentEquals(ResponseApdu.OK.toByteArray(), service.processCommandApdu(selectCommand, null))

        channels.detachEngine()
        assertContentEquals(ResponseApdu.HCE_NOT_READY.toByteArray(), service.processCommandApdu(selectCommand, null))

        deliverListen()
        assertContentEquals(ResponseApdu.HCE_NOT_READY.toByteArray(), service.processCommandApdu(selectCommand, null))

        val otherSink = mock<EventSink>()
        HostCardEmulationStreamHandler().onListen(hceArguments, otherSink)
        deliverListen()
        assertContentEquals(ResponseApdu.OK.toByteArray(), service.processCommandApdu(selectCommand, null))
        verify(otherSink).success(HostCardEmulationStatus.AID_SELECTED.name)
    }

    /** Attaches [plugin] to an engine with a mock messenger and records each channel's handler. */
    private inner class PluginChannels {
        val messenger = mock<BinaryMessenger>()
        private val handlers = mutableMapOf<String, BinaryMessenger.BinaryMessageHandler?>()
        private val context = mock<Context> { on { packageManager } doReturn mock<PackageManager>() }
        private val binding =
            mock<FlutterPlugin.FlutterPluginBinding> {
                on { applicationContext } doReturn context
                on { binaryMessenger } doReturn messenger
            }

        init {
            whenever(messenger.setMessageHandler(any(), anyOrNull())).thenAnswer {
                handlers[it.getArgument(0)] = it.getArgument(1)
                null
            }
            mockStatic(NfcAdapter::class.java).use {
                it.`when`<NfcAdapter> { NfcAdapter.getDefaultAdapter(context) }.thenReturn(mock<NfcAdapter>())
                plugin.onAttachedToEngine(binding)
            }
        }

        fun handler(channel: String) = assertNotNull(handlers[channel])

        /** Sends a method call to [channel] and returns the reply, or null if none was sent. */
        fun call(
            channel: String,
            method: String,
            arguments: Any? = null,
        ) = handlers[channel]?.let { send(it, method, arguments) }

        fun send(
            handler: BinaryMessenger.BinaryMessageHandler,
            method: String,
            arguments: Any?,
        ): ByteBuffer? {
            var reply: ByteBuffer? = null
            handler.onMessage(methodCodec.encodeMethodCall(MethodCall(method, arguments)).apply { flip() }) {
                reply = it?.apply { flip() }
            }
            return reply
        }

        /** Sends a Pigeon request to [method] and returns the decoded reply: `[result]`, or `[code, message, details]` for an error. */
        fun request(
            method: String,
            vararg arguments: Any?,
        ): List<*> {
            var reply: List<*>? = null
            handler(hostApiChannel(method)).onMessage(
                assertNotNull(NfcManagerHostApi.codec.encodeMessage(arguments.toList())).apply { flip() },
            ) { reply = assertIs<List<*>>(NfcManagerHostApi.codec.decodeMessage(it?.apply { flip() })) }
            return assertNotNull(reply)
        }

        fun attachActivity() = plugin.onAttachedToActivity(activityBinding())

        fun reattachActivityForConfigChanges() = plugin.onReattachedToActivityForConfigChanges(activityBinding())

        fun detachEngine() = plugin.onDetachedFromEngine(binding)

        private fun activityBinding() = mock<ActivityPluginBinding> { on { activity } doReturn mock<Activity>() }
    }

    private companion object {
        val HOST_API_METHODS = listOf("isHceSupported", "isNfcSupported", "isNfcEnabled", "sendCommand")
        const val DISCOVERY_CHANNEL = "dev.huynh/nfc_manager_android/discovery"
        const val HCE_CHANNEL = "dev.huynh/nfc_manager_android/host_card_emulation"
        val methodCodec: StandardMethodCodec = StandardMethodCodec.INSTANCE

        fun hostApiChannel(method: String) = "dev.flutter.pigeon.nfc_manager_android.NfcManagerHostApi.$method"
    }
}
