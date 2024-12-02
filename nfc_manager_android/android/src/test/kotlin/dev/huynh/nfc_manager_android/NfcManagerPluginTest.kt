import dev.huynh.nfc_manager_android.NfcManagerPlugin
import dev.huynh.nfc_manager_android.NfcService
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.Result
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NfcManagerPluginTest {
    private val mockNfcService = mock<NfcService>()
    private val plugin = NfcManagerPlugin().apply { nfcService = mockNfcService }
    private val mockResult = mock<Result>()

    @Test
    fun `isHceSupported method call returns correctly`() {
        whenever(mockNfcService.isHceSupported()).thenReturn(true)
        plugin.onMethodCall(
            MethodCall("isHceSupported", null),
            mockResult,
        )
        verify(mockResult).success(true)
    }

    @Test
    fun `isNfcSupported method call returns correctly`() {
        whenever(mockNfcService.isNfcSupported()).thenReturn(true)
        plugin.onMethodCall(
            MethodCall("isNfcSupported", null),
            mockResult,
        )
        verify(mockResult).success(true)
    }

    @Test
    fun `isNfcEnabled method call returns correctly`() {
        whenever(mockNfcService.isNfcEnabled()).thenReturn(true)
        plugin.onMethodCall(
            MethodCall("isNfcEnabled", null),
            mockResult,
        )
        verify(mockResult).success(true)
    }
}
