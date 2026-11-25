package dev.huynh.nfc_manager_android

import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureCheckerTest {
    private val mockPackageManager = mock<PackageManager>()
    private val mockNfcAdapter = mock<NfcAdapter>()
    private val featureChecker = FeatureChecker(nfcAdapter = mockNfcAdapter, packageManager = mockPackageManager)

    @Test
    fun `isHceSupported returns the HCE system feature`() =
        listOf(true, false).forEach { supported ->
            whenever(mockPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)).thenReturn(supported)

            assertEquals(supported, featureChecker.isHceSupported())
        }

    @Test
    fun `isNfcEnabled returns the adapter state`() =
        listOf(true, false).forEach { enabled ->
            whenever(mockNfcAdapter.isEnabled).thenReturn(enabled)

            assertEquals(enabled, featureChecker.isNfcEnabled())
        }

    @Test
    fun `isNfcSupported returns true with an adapter`() = assertTrue(featureChecker.isNfcSupported())

    @Test
    fun `without an adapter NFC is unsupported and disabled`() =
        FeatureChecker(nfcAdapter = null, packageManager = mockPackageManager).run {
            assertFalse(isNfcSupported())
            assertFalse(isNfcEnabled())
        }
}
