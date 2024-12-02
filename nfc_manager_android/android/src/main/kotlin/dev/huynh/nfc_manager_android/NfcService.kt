package dev.huynh.nfc_manager_android

import android.nfc.NfcAdapter

class NfcService(private val nfcAdapter: NfcAdapter?) {
    fun isNfcSupported(): Boolean = nfcAdapter != null
}