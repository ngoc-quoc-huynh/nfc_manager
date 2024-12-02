package dev.huynh.nfc_manager.nfc_manager_android

import android.nfc.NfcAdapter
import dev.huynh.nfc_manager_android.NfcService
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class NfcServiceTest {
    @Test
    fun `isNfcSupported returns true when NfcAdapter is not null`() {
        val nfcAdapter = mock<NfcAdapter>()
        val nfcService = NfcService(nfcAdapter)
        assertTrue(nfcService.isNfcSupported())
    }

    @Test
    fun `isNfcSupported returns false when NfcAdapter is null`() {
        val nfcAdapter = null
        val nfcService = NfcService(nfcAdapter)
        assertFalse(nfcService.isNfcSupported())
    }
}