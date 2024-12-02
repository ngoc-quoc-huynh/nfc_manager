package dev.huynh.nfc_manager_android

import android.content.pm.PackageManager
import android.nfc.NfcAdapter

class NfcService(
    private val nfcAdapter: NfcAdapter?,
    private val packageManager: PackageManager,
) {
    fun isHceSupported(): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)

    fun isNfcSupported(): Boolean = nfcAdapter != null
}
