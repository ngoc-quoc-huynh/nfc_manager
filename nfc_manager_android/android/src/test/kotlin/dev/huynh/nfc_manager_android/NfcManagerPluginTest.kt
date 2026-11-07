import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.NfcManagerPlugin
import dev.huynh.nfc_manager_android.TagConnectionException
import dev.huynh.nfc_manager_android.TagReader
import dev.huynh.nfc_manager_android.feature_checker.FeatureChecker
import dev.huynh.nfc_manager_android.models.ResponseApdu
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NfcManagerPluginTest {
    private val mockFeatureChecker = mock<FeatureChecker>()
    private val mockTagReader = mock<TagReader>()
    private val plugin =
        NfcManagerPlugin().apply {
            featureChecker = mockFeatureChecker
            tagReader = mockTagReader
        }
    private val mockResult = mock<Result>()

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
        mapOf(
            "methodChannel" to mockMethodChannel,
            "discoveryEventChannel" to mockDiscoveryEventChannel,
            "hostCardEmulationEventChannel" to mockHostCardEmulationEventChannel,
        ).forEach { (fieldName, value) ->
            NfcManagerPlugin::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
                set(plugin, value)
            }
        }

        plugin.onDetachedFromEngine(mock<FlutterPlugin.FlutterPluginBinding>())

        verify(mockMethodChannel).setMethodCallHandler(null)
        verify(mockDiscoveryEventChannel).setStreamHandler(null)
        verify(mockHostCardEmulationEventChannel).setStreamHandler(null)
        verify(mockTagReader).dispose()
    }

    @Test
    fun `throws not implemented for other method calls`() {
        plugin.onMethodCall(
            MethodCall("other", null),
            mockResult,
        )
        verify(mockResult).notImplemented()
    }
}
