import dev.huynh.nfc_manager_android.NfcManagerPlugin
import dev.huynh.nfc_manager_android.NfcNotSupportedException
import dev.huynh.nfc_manager_android.TagConnectionException
import dev.huynh.nfc_manager_android.TagReader
import dev.huynh.nfc_manager_android.feature_checker.FeatureChecker
import dev.huynh.nfc_manager_android.host_card_emulation.HostCardEmulation
import dev.huynh.nfc_manager_android.models.ResponseApdu
import dev.huynh.nfc_manager_android.utils.error
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.Result
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NfcManagerPluginTest {
    private val mockFeatureChecker = mock<FeatureChecker>()
    private val mockHostCardEmulation = mock<HostCardEmulation>()
    private val mockTagReader = mock<TagReader>()
    private val plugin =
        NfcManagerPlugin().apply {
            hostCardEmulation = mockHostCardEmulation
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
    fun `startDiscovery method call returns correctly`() {
        plugin.onMethodCall(
            MethodCall("startDiscovery", null),
            mockResult,
        )

        verify(mockResult).success(null)
    }

    @Test
    fun `startDiscovery method call returns error if exception is thrown`() {
        doAnswer { throw NfcNotSupportedException() }
            .whenever(mockTagReader)
            .startDiscovery()
        plugin.onMethodCall(
            MethodCall("startDiscovery", null),
            mockResult,
        )

        verify(mockResult).error(NfcNotSupportedException())
    }

    @Test
    fun `stopDiscovery method call returns correctly`() {
        plugin.onMethodCall(
            MethodCall("stopDiscovery", null),
            mockResult,
        )

        verify(mockResult).success(null)
    }

    @Test
    fun `stopDiscovery method call returns error if exception is thrown`() {
        doAnswer { throw NfcNotSupportedException() }
            .whenever(mockTagReader)
            .stopDiscovery()
        plugin.onMethodCall(
            MethodCall("stopDiscovery", null),
            mockResult,
        )

        verify(mockResult).error(NfcNotSupportedException())
    }

    @Test
    fun `sendCommand method call returns correctly`() {
        whenever(mockTagReader.sendCommand(byteArrayOf())).thenReturn(ResponseApdu.OK())
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
        whenever(mockTagReader.sendCommand(byteArrayOf())).thenAnswer {
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
    fun `startEmulation method call returns correctly`() {
        plugin.onMethodCall(
            MethodCall(
                "startEmulation",
                mapOf(
                    "aid" to byteArrayOf(),
                    "pin" to byteArrayOf(),
                ),
            ),
            mockResult,
        )
        verify(mockResult).success(null)
    }

    @Test
    fun `stopEmulation method call returns correctly`() {
        plugin.onMethodCall(
            MethodCall("stopEmulation", null),
            mockResult,
        )
        verify(mockResult).success(null)
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
