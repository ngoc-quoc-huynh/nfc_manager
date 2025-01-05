package dev.huynh.nfc_manager_android.feature_checker

import android.content.pm.PackageManager
import android.nfc.NfcAdapter

/**
 * Provides utility methods to check the availability and status of NFC and HCE features on the device.
 * 
 * This class allows querying whether the device hardware supports NFC, if NFC is currently enabled,
 * and whether the device supports NFC Host Card Emulation (HCE).
 */
class FeatureChecker(
    private val nfcAdapter: NfcAdapter?,
    private val packageManager: PackageManager,
) {
    /**
     * Checks if the device supports HCE.
     */
    fun isHceSupported(): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)

    /**
     * Checks if the device supports NFC.
     */
    fun isNfcSupported(): Boolean = nfcAdapter != null

    /**
     * Checks if NFC is enabled.
     */
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true
}
