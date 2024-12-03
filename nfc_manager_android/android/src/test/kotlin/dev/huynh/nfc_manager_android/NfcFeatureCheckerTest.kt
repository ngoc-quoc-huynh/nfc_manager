package dev.huynh.nfc_manager.nfc_manager_android

import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import dev.huynh.nfc_manager_android.NfcFeatureChecker
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class NfcFeatureCheckerTest {
    private val mockPackageManager = mock<PackageManager>()
    private val mockNfcAdapter = mock<NfcAdapter>()
    private val nfcFeatureChecker =
        NfcFeatureChecker(
            packageManager = mockPackageManager,
            nfcAdapter = mockNfcAdapter,
        )

    @Test
    fun `isHceSupported returns true when device supports NFC host card emulation`() {
        whenever(
            mockPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION),
        ).thenReturn(
            true,
        )
        assertTrue(nfcFeatureChecker.isHceSupported())
    }

    @Test
    fun `isHceSupported returns false when device does not supports NFC host card emulation`() {
        whenever(
            mockPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION),
        ).thenReturn(
            false,
        )
        assertFalse(nfcFeatureChecker.isHceSupported())
    }

    @Test
    fun `isNfcSupported returns true when NfcAdapter is not null`() =
        assertTrue(
            nfcFeatureChecker.isNfcSupported(),
        )

    @Test
    fun `isNfcSupported returns false when NfcAdapter is null`() {
        val nfcFeatureChecker =
            NfcFeatureChecker(
                packageManager = mockPackageManager,
                nfcAdapter = null,
            )
        assertFalse(nfcFeatureChecker.isNfcSupported())
    }

    @Test
    fun `isNfcEnabled returns true when NFC is enabled`() {
        whenever(mockNfcAdapter.isEnabled).thenReturn(true)
        assertTrue(nfcFeatureChecker.isNfcEnabled())
    }

    @Test
    fun `isNfcEnabled returns false when NfcAdapter is null`() {
        val nfcFeatureChecker =
            NfcFeatureChecker(
                packageManager = mockPackageManager,
                nfcAdapter = null,
            )
        assertFalse(nfcFeatureChecker.isNfcSupported())
    }

    @Test
    fun `isNfcEnabled returns false when NFC is disabled`() {
        whenever(mockNfcAdapter.isEnabled).thenReturn(false)
        assertFalse(nfcFeatureChecker.isNfcEnabled())
    }
}
