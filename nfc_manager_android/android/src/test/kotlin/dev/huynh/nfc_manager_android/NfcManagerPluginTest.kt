import dev.huynh.nfc_manager_android.NfcManagerPlugin
import dev.huynh.nfc_manager_android.NfcReader
import dev.huynh.nfc_manager_android.feature_checker.NfcFeatureChecker
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.Result
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NfcManagerPluginTest {
    private val mockNfcFeatureChecker = mock<NfcFeatureChecker>()
    private val mockNfcReader = mock<NfcReader>()
    private val plugin =
        NfcManagerPlugin().apply {
            nfcFeatureChecker = mockNfcFeatureChecker
            nfcReader = mockNfcReader
        }
    private val mockResult = mock<Result>()

    @Test
    fun `isHceSupported method call returns correctly`() {
        whenever(mockNfcFeatureChecker.isHceSupported()).thenReturn(true)
        plugin.onMethodCall(
            MethodCall("isHceSupported", null),
            mockResult,
        )
        verify(mockResult).success(true)
    }

    @Test
    fun `isNfcSupported method call returns correctly`() {
        whenever(mockNfcFeatureChecker.isNfcSupported()).thenReturn(true)
        plugin.onMethodCall(
            MethodCall("isNfcSupported", null),
            mockResult,
        )
        verify(mockResult).success(true)
    }

    @Test
    fun `isNfcEnabled method call returns correctly`() {
        whenever(mockNfcFeatureChecker.isNfcEnabled()).thenReturn(true)
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
    fun `stopDiscovery method call returns correctly`() {
        plugin.onMethodCall(
            MethodCall("stopDiscovery", null),
            mockResult,
        )
        verify(mockResult).success(null)
    }
}
