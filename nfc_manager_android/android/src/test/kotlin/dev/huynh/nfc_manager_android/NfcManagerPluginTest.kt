package dev.huynh.nfc_manager_android

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.util.Log
import dev.huynh.nfc_manager_android.feature.FeatureChecker
import dev.huynh.nfc_manager_android.hce.HostCardEmulationConfig
import dev.huynh.nfc_manager_android.hce.HostCardEmulationService
import dev.huynh.nfc_manager_android.hce.HostCardEmulationStreamHandler
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.models.ResponseApdu
import dev.huynh.nfc_manager_android.reader.TagReader
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.FlutterException
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NfcManagerPluginTest {
    private val mockFeatureChecker = mock<FeatureChecker>()
    private val mockTagReader = mock<TagReader>()
    private val plugin =
        NfcManagerPlugin().apply {
            featureChecker = mockFeatureChecker
            tagReader = mockTagReader
        }
    private val mockResult = mock<Result>()
    private val aid = byteArrayOf(0xF0.toByte(), 0x01, 0x02, 0x03, 0x04)
    private val hceArguments = mapOf("aid" to aid, "pin" to byteArrayOf(0x02))
    private val selectCommand = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid

    @AfterEach
    fun tearDown() {
        HostCardEmulationConfig.session?.let { HostCardEmulationConfig.clear(it.owner) }
    }

    @Test
    fun `isHceSupported method call returns correctly`() {
        whenever(mockFeatureChecker.isHceSupported()).thenReturn(true)
        plugin.onMethodCall(
            MethodCall("isHceSupported", null),
            mockResult,
        )

        verify(mockResult).success(true)
    }

    @Test
    fun `isNfcSupported method call returns correctly`() {
        whenever(mockFeatureChecker.isNfcSupported()).thenReturn(true)
        plugin.onMethodCall(
            MethodCall("isNfcSupported", null),
            mockResult,
        )

        verify(mockResult).success(true)
    }

    @Test
    fun `isNfcEnabled method call returns correctly`() {
        whenever(mockFeatureChecker.isNfcEnabled()).thenReturn(true)
        plugin.onMethodCall(
            MethodCall("isNfcEnabled", null),
            mockResult,
        )

        verify(mockResult).success(true)
    }

    @Test
    fun `sendCommand method call returns correctly`() {
        whenever(mockTagReader.sendCommand(any(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<(ByteArray) -> Unit>(1)(ResponseApdu.OK.toByteArray())
        }
        plugin.onMethodCall(
            MethodCall(
                "sendCommand",
                mapOf("command" to byteArrayOf()),
            ),
            mockResult,
        )

        verify(mockResult).success(ResponseApdu.OK.toByteArray())
    }

    @Test
    fun `sendCommand method call returns error if method throws`() {
        whenever(mockTagReader.sendCommand(any(), any(), any())).thenAnswer {
            throw TagConnectionException()
        }
        plugin.onMethodCall(
            MethodCall(
                "sendCommand",
                mapOf("command" to byteArrayOf()),
            ),
            mockResult,
        )

        verify(mockResult).error(TagConnectionException())
    }

    @Test
    fun `sendCommand method call returns error from asynchronous NFC failure`() {
        whenever(mockTagReader.sendCommand(any(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<(Throwable) -> Unit>(2)(TagConnectionException())
        }
        plugin.onMethodCall(
            MethodCall("sendCommand", mapOf("command" to byteArrayOf())),
            mockResult,
        )

        verify(mockResult).error(TagConnectionException())
    }

    @Test
    fun `sendCommand reports unexpected asynchronous failures`() {
        whenever(mockTagReader.sendCommand(any(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<(Throwable) -> Unit>(2)(IllegalStateException())
        }
        plugin.onMethodCall(
            MethodCall("sendCommand", mapOf("command" to byteArrayOf())),
            mockResult,
        )

        verify(mockResult).error("NFC_ERROR", "Unexpected NFC reader error.", null)
    }

    @Test
    fun `sendCommand method call rejects a missing command`() {
        plugin.onMethodCall(MethodCall("sendCommand", null), mockResult)

        verify(mockResult).error(InvalidArgumentsException())
    }

    @Test
    fun `sendCommand method call rejects a malformed command`() {
        plugin.onMethodCall(MethodCall("sendCommand", mapOf("command" to "invalid")), mockResult)

        verify(mockResult).error(InvalidArgumentsException())
    }

    @Test
    fun `discovery reports an error and ends when started without an activity`() {
        val channel = DiscoveryChannel()
        plugin.tagReader = null

        assertNull(StandardMethodCodec.INSTANCE.decodeEnvelope(assertNotNull(channel.call("listen"))))

        val event = argumentCaptor<ByteBuffer>()
        verify(channel.messenger, times(2)).send(eq(channel.name), event.capture())
        assertEquals(1.toByte(), assertNotNull(event.firstValue).apply { flip() }.get())
        assertNull(event.lastValue)
    }

    @Test
    fun `discovery accepts cancellation after an activity detaches`() {
        val channel = DiscoveryChannel()
        channel.attachActivity()
        assertNull(StandardMethodCodec.INSTANCE.decodeEnvelope(assertNotNull(channel.call("listen"))))

        plugin.onDetachedFromActivity()

        val reply = assertNotNull(channel.call("cancel"))
        assertNull(StandardMethodCodec.INSTANCE.decodeEnvelope(reply))
    }

    @Test
    fun `discovery delivers events from the reader after activity reattachment`() {
        val channel = DiscoveryChannel()
        channel.attachActivity()
        assertNotNull(channel.call("listen"))
        val previousReader = plugin.tagReader
        plugin.onDetachedFromActivityForConfigChanges()
        assertNotNull(channel.call("cancel"))

        channel.reattachActivityForConfigChanges()
        assertNotNull(channel.call("listen"))
        val reader = assertNotNull(plugin.tagReader)
        assertNull(previousReader?.eventSink)
        assertNotNull(reader.eventSink).success("04A1")

        val event = argumentCaptor<ByteBuffer>()
        verify(channel.messenger, times(2)).send(eq(channel.name), event.capture())
        assertNull(event.firstValue)
        assertEquals("04A1", StandardMethodCodec.INSTANCE.decodeEnvelope(event.lastValue.apply { flip() }))
        plugin.onDetachedFromActivity()
    }

    @Test
    fun `activity detachment cancels and releases the tag reader`() {
        plugin.onDetachedFromActivity()

        verify(mockTagReader).dispose()
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
    fun `engine detachment cancels and releases the tag reader`() {
        val mockMethodChannel = mock<MethodChannel>()
        val mockDiscoveryEventChannel = mock<EventChannel>()
        val mockHostCardEmulationEventChannel = mock<EventChannel>()
        setChannels(mockMethodChannel, mockDiscoveryEventChannel, mockHostCardEmulationEventChannel)

        plugin.onDetachedFromEngine(mock<FlutterPlugin.FlutterPluginBinding>())

        verify(mockMethodChannel).setMethodCallHandler(null)
        verify(mockDiscoveryEventChannel).setStreamHandler(null)
        verify(mockHostCardEmulationEventChannel).setStreamHandler(null)
        verify(mockTagReader).dispose()
    }

    @Test
    fun `engine detachment during emulation stops HCE responses`() {
        val mockEventSink = mock<EventSink>()
        val streamHandler = HostCardEmulationStreamHandler()
        plugin.hostCardEmulationStreamHandler = streamHandler
        setChannels()
        streamHandler.onListen(hceArguments, mockEventSink)

        plugin.onDetachedFromEngine(mock<FlutterPlugin.FlutterPluginBinding>())
        val response = HostCardEmulationService().processCommandApdu(selectCommand, null)

        assertContentEquals(ResponseApdu.HCE_NOT_READY.toByteArray(), response)
        assertNull(plugin.hostCardEmulationStreamHandler)
        verify(mockEventSink).success(HostCardEmulationStatus.READY.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `engine detachment keeps an HCE session started by another engine`() {
        val mockEventSink = mock<EventSink>()
        plugin.hostCardEmulationStreamHandler = HostCardEmulationStreamHandler()
        setChannels()
        HostCardEmulationStreamHandler().onListen(hceArguments, mockEventSink)

        plugin.onDetachedFromEngine(mock<FlutterPlugin.FlutterPluginBinding>())
        val response = HostCardEmulationService().processCommandApdu(selectCommand, null)

        assertContentEquals(ResponseApdu.OK.toByteArray(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.AID_SELECTED.name)
    }

    @Test
    fun `HCE rejects an invalid AID length and accepts the next listen`() {
        val channel = "dev.huynh/nfc_manager_android/host_card_emulation"
        val codec = StandardMethodCodec.INSTANCE
        val messenger = mock<BinaryMessenger>()
        val context = mock<Context> { on { packageManager } doReturn mock<PackageManager>() }
        val binding =
            mock<FlutterPlugin.FlutterPluginBinding> {
                on { applicationContext } doReturn context
                on { binaryMessenger } doReturn messenger
            }
        mockStatic(NfcAdapter::class.java).use { plugin.onAttachedToEngine(binding) }
        val handler = argumentCaptor<BinaryMessenger.BinaryMessageHandler>()
        verify(messenger).setMessageHandler(eq(channel), handler.capture())

        fun listen(arguments: Map<String, ByteArray>) {
            val message = codec.encodeMethodCall(MethodCall("listen", arguments)).apply { flip() }
            handler.firstValue.onMessage(message) { reply ->
                assertNull(codec.decodeEnvelope(assertNotNull(reply).apply { flip() }))
            }
        }

        listen(hceArguments + ("aid" to ByteArray(4)))
        assertNull(HostCardEmulationConfig.session)

        listen(hceArguments)
        kotlin.test.assertSame<EventChannel.StreamHandler?>(
            plugin.hostCardEmulationStreamHandler,
            assertNotNull(HostCardEmulationConfig.session).owner,
        )

        val events = argumentCaptor<ByteBuffer>()
        verify(messenger, times(3)).send(eq(channel), events.capture())
        val invalidArguments =
            assertFailsWith<FlutterException> {
                codec.decodeEnvelope(assertNotNull(events.firstValue).apply { flip() })
            }
        assertEquals("INVALID_ARGUMENTS", invalidArguments.code)
        assertEquals("HCE requires an AID of 5–16 bytes and a PIN of 1–255 bytes.", invalidArguments.message)
        assertNull(invalidArguments.details)
        assertNull(events.allValues[1])
        assertEquals("READY", codec.decodeEnvelope(assertNotNull(events.lastValue).apply { flip() }))
    }

    @Test
    fun `a queued HCE listen cannot restore or replace a session after engine detachment`() {
        val messenger = mock<BinaryMessenger>()
        val context = mock<Context>()
        whenever(context.packageManager).thenReturn(mock<PackageManager>())
        val binding = mock<FlutterPlugin.FlutterPluginBinding>()
        whenever(binding.applicationContext).thenReturn(context)
        whenever(binding.binaryMessenger).thenReturn(messenger)
        mockStatic(NfcAdapter::class.java).use { plugin.onAttachedToEngine(binding) }
        val handler = argumentCaptor<BinaryMessenger.BinaryMessageHandler>()
        verify(messenger).setMessageHandler(eq("dev.huynh/nfc_manager_android/host_card_emulation"), handler.capture())

        fun deliverListen() {
            val message = StandardMethodCodec.INSTANCE.encodeMethodCall(MethodCall("listen", hceArguments))
            message.rewind()
            mockStatic(Log::class.java).use { handler.firstValue.onMessage(message, mock()) }
        }

        val service = HostCardEmulationService()
        deliverListen()
        assertContentEquals(ResponseApdu.OK.toByteArray(), service.processCommandApdu(selectCommand, null))

        plugin.onDetachedFromEngine(binding)
        assertContentEquals(ResponseApdu.HCE_NOT_READY.toByteArray(), service.processCommandApdu(selectCommand, null))

        deliverListen()
        assertContentEquals(ResponseApdu.HCE_NOT_READY.toByteArray(), service.processCommandApdu(selectCommand, null))

        val otherSink = mock<EventSink>()
        HostCardEmulationStreamHandler().onListen(hceArguments, otherSink)
        deliverListen()
        assertContentEquals(ResponseApdu.OK.toByteArray(), service.processCommandApdu(selectCommand, null))
        verify(otherSink).success(HostCardEmulationStatus.AID_SELECTED.name)
    }

    @Test
    fun `throws not implemented for other method calls`() {
        plugin.onMethodCall(
            MethodCall("other", null),
            mockResult,
        )
        verify(mockResult).notImplemented()
    }

    private inner class DiscoveryChannel {
        val name = "dev.huynh/nfc_manager_android/discovery"
        val messenger = mock<BinaryMessenger>()
        private val adapter = mock<NfcAdapter>()
        private var handler: BinaryMessenger.BinaryMessageHandler? = null

        init {
            whenever(messenger.setMessageHandler(eq(name), anyOrNull())).thenAnswer {
                handler = it.getArgument(1)
                null
            }
            val context = mock<Context> { on { packageManager } doReturn mock<PackageManager>() }
            val binding =
                mock<FlutterPlugin.FlutterPluginBinding> {
                    on { applicationContext } doReturn context
                    on { binaryMessenger } doReturn messenger
                }
            mockStatic(NfcAdapter::class.java).use {
                it.`when`<NfcAdapter> { NfcAdapter.getDefaultAdapter(context) }.thenReturn(adapter)
                plugin.onAttachedToEngine(binding)
            }
        }

        fun attachActivity() {
            plugin.onAttachedToActivity(activityBinding())
        }

        fun reattachActivityForConfigChanges() {
            plugin.onReattachedToActivityForConfigChanges(activityBinding())
        }

        fun call(method: String): ByteBuffer? {
            val codec = StandardMethodCodec.INSTANCE
            var reply: ByteBuffer? = null
            handler?.onMessage(codec.encodeMethodCall(MethodCall(method, null)).apply { flip() }) {
                reply = it?.apply { flip() }
            }
            return reply
        }

        private fun activityBinding() =
            mock<ActivityPluginBinding> {
                on { activity } doReturn mock<Activity>()
            }
    }

    private fun setChannels(
        methodChannel: MethodChannel = mock(),
        discoveryEventChannel: EventChannel = mock(),
        hostCardEmulationEventChannel: EventChannel = mock(),
    ) = mapOf(
        "methodChannel" to methodChannel,
        "discoveryEventChannel" to discoveryEventChannel,
        "hostCardEmulationEventChannel" to hostCardEmulationEventChannel,
    ).forEach { (fieldName, value) ->
        NfcManagerPlugin::class.java.getDeclaredField(fieldName).apply {
            isAccessible = true
            set(plugin, value)
        }
    }
}
