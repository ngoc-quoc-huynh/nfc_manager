package dev.huynh.nfc_manager_android

import android.app.Activity
import android.nfc.NfcAdapter
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import org.jetbrains.annotations.VisibleForTesting

class NfcReader(
    private val activity: Activity,
    private val nfcAdapter: NfcAdapter?,
) : StreamHandler {
    @VisibleForTesting
    var eventSink: EventSink? = null

    @VisibleForTesting
    var isDiscoveryStarted = false

    fun startDiscovery() {
        if (nfcAdapter == null) throw NfcNotSupportedException()

        if (isDiscoveryStarted) {
            return
        }

        isDiscoveryStarted = true
        nfcAdapter.enableReaderMode(
            activity,
            { tag ->
                tag?.let {
                    val tagId = it.id.toHex()
                    activity.runOnUiThread { eventSink?.success(tagId) }
                }
            },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null,
        )
    }

    fun stopDiscovery() {
        if (nfcAdapter == null) throw NfcNotSupportedException()

        eventSink = null
        if (isDiscoveryStarted) {
            nfcAdapter.disableReaderMode(activity)
            isDiscoveryStarted = false
        }
    }

    override fun onListen(
        arguments: Any?,
        events: EventSink?,
    ) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
        stopDiscovery()
    }
}
