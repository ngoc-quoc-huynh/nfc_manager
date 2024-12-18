package dev.huynh.nfc_manager_android

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import androidx.annotation.VisibleForTesting
import dev.huynh.nfc_manager_android.utils.error
import dev.huynh.nfc_manager_android.utils.toHexString
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import java.io.IOException

class TagReader(
    private val activity: Activity,
    private val nfcAdapter: NfcAdapter?,
) : StreamHandler {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var eventSink: EventSink? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var isDiscoveryStarted = false

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var isoDep: IsoDep? = null

    fun startDiscovery() {
        val adapter = getNfcAdapter()

        if (isDiscoveryStarted) return

        isDiscoveryStarted = true
        adapter.enableReaderMode(
            activity,
            ::onTagFound,
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B,
            null,
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onTagFound(tag: Tag?) {
        if (tag == null) return

        val isoDep =
            IsoDep.get(tag) ?: run {
                return emitError(IsoDepNotSupportedException())
            }

        runCatching {
            isoDep.connect()
            isoDep.timeout = 3000
            val tagId = tag.id.toHexString()
            emitSuccess(tagId)
        }.onFailure { exception ->
            if (exception is IOException) {
                emitError(TagConnectionException())
            }
        }
    }

    fun stopDiscovery() {
        val adapter = getNfcAdapter()

        eventSink = null
        if (isDiscoveryStarted) {
            adapter.disableReaderMode(activity)
            isDiscoveryStarted = false
            isoDep?.close()
            isoDep = null
        }
    }

    fun sendCommand(command: ByteArray): ByteArray {
        val currentIsoDep = isoDep ?: throw TagConnectionException()

        return try {
            currentIsoDep.transceive(command)
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

    private fun getNfcAdapter(): NfcAdapter = nfcAdapter ?: throw NfcNotSupportedException()

    private fun emitSuccess(data: Any) {
        activity.runOnUiThread {
            eventSink?.success(data)
        }
    }

    private fun emitError(exception: NfcException) =
        activity.runOnUiThread {
            eventSink?.error(exception)
        }
}
