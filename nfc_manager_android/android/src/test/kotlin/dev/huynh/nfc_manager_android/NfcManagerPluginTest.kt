import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.util.Log
import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.NfcManagerPlugin
import dev.huynh.nfc_manager_android.TagConnectionException
import dev.huynh.nfc_manager_android.TagReader
import dev.huynh.nfc_manager_android.feature_checker.FeatureChecker
import dev.huynh.nfc_manager_android.host_card_emulation.HostCardEmulation
import dev.huynh.nfc_manager_android.host_card_emulation.HostCardEmulationConfig
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.models.ResponseApdu
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.StandardMethodCodec
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import kotlin.test.assertContentEquals
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
    private val hceArguments = mapOf("aid" to byteArrayOf(0x01), "pin" to byteArrayOf(0x02))
    private val selectCommand = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x01, 0x01)

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
            invocation.getArgument<(ByteArray) -> Unit>(1)(ResponseApdu.OK())
        }
        plugin.onMethodCall(
            MethodCall(
                "sendCommand",
                mapOf("command" to byteArrayOf()),
            ),
            mockResult,
        )

        verify(mockResult).success(ResponseApdu.OK())
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

        verify(mockResult).error(InvalidApduCommandException())
    }

    @Test
    fun `sendCommand method call rejects a malformed command`() {
        plugin.onMethodCall(MethodCall("sendCommand", mapOf("command" to "invalid")), mockResult)

        verify(mockResult).error(InvalidApduCommandException())
    }

    @Test
    fun `activity detachment cancels and releases the tag reader`() {
        val mockDiscoveryEventChannel = mock<EventChannel>()
        NfcManagerPlugin::class.java.getDeclaredField("discoveryEventChannel").apply {
            isAccessible = true
            set(plugin, mockDiscoveryEventChannel)
        }

        plugin.onDetachedFromActivity()

        verify(mockDiscoveryEventChannel).setStreamHandler(null)
        verify(mockTagReader).dispose()
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
        val streamHandler = HostCardEmulation()
        plugin.hostCardEmulationStreamHandler = streamHandler
        setChannels()
        streamHandler.onListen(hceArguments, mockEventSink)

        plugin.onDetachedFromEngine(mock<FlutterPlugin.FlutterPluginBinding>())
        val response = HostCardEmulation().processCommandApdu(selectCommand, null)

        assertContentEquals(ResponseApdu.HCE_NOT_READY(), response)
        assertNull(plugin.hostCardEmulationStreamHandler)
        verify(mockEventSink).success(HostCardEmulationStatus.READY.name)
        verifyNoMoreInteractions(mockEventSink)
    }

    @Test
    fun `engine detachment keeps an HCE session started by another engine`() {
        val mockEventSink = mock<EventSink>()
        plugin.hostCardEmulationStreamHandler = HostCardEmulation()
        setChannels()
        HostCardEmulation().onListen(hceArguments, mockEventSink)

        plugin.onDetachedFromEngine(mock<FlutterPlugin.FlutterPluginBinding>())
        val response = HostCardEmulation().processCommandApdu(selectCommand, null)

        assertContentEquals(ResponseApdu.OK(), response)
        verify(mockEventSink).success(HostCardEmulationStatus.AID_SELECTED.name)
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

        val service = HostCardEmulation()
        deliverListen()
        assertContentEquals(ResponseApdu.OK(), service.processCommandApdu(selectCommand, null))

        plugin.onDetachedFromEngine(binding)
        assertContentEquals(ResponseApdu.HCE_NOT_READY(), service.processCommandApdu(selectCommand, null))

        deliverListen()
        assertContentEquals(ResponseApdu.HCE_NOT_READY(), service.processCommandApdu(selectCommand, null))

        val otherSink = mock<EventSink>()
        HostCardEmulation().onListen(hceArguments, otherSink)
        deliverListen()
        assertContentEquals(ResponseApdu.OK(), service.processCommandApdu(selectCommand, null))
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
