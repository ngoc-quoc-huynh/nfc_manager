package dev.huynh.nfc_manager_android

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import dev.huynh.nfc_manager_android.utils.error
import dev.huynh.nfc_manager_android.utils.toHexString
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import org.jetbrains.annotations.VisibleForTesting
import java.io.IOException

class TagReader(
    private val activity: Activity,
    private val nfcAdapter: NfcAdapter?,
) : StreamHandler {
    @VisibleForTesting
    var eventSink: EventSink? = null

    @VisibleForTesting
    var isDiscoveryStarted = false

    private var isoDep: IsoDep? = null

    fun startDiscovery() {
        if (nfcAdapter == null) throw NfcNotSupportedException()

        if (isDiscoveryStarted) {
            return
        }

        isDiscoveryStarted = true
        nfcAdapter.enableReaderMode(
            activity,
            ::onTagFound,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null,
        )
    }

    private fun onTagFound(tag: Tag?) {
        if (tag == null) return

        val isoDep = IsoDep.get(tag) ?: throw IsoDepNotSupportedException()

        runCatching {
            isoDep.connect()
            val tagId = tag.id.toHexString()
            activity.runOnUiThread { eventSink?.success(tagId) }
        }.onFailure { exception ->
            if (exception is IOException) {
                activity.runOnUiThread {
                    eventSink?.error(TagConnectionException())
                }
            }
        }
    }

    fun stopDiscovery() {
        if (nfcAdapter == null) throw NfcNotSupportedException()

        eventSink = null
        if (isDiscoveryStarted) {
            nfcAdapter.disableReaderMode(activity)
            isDiscoveryStarted = false
        }
    }

    fun sendCommand(command: ByteArray): ByteArray {
        if (isoDep == null) throw TagConnectionException()

        try {
            return isoDep!!.transceive(command)
        } catch (e: TagLostException) {
            throw TagConnectionException()
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
