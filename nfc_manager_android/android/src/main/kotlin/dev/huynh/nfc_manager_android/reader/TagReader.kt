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
    @VisibleForTesting
    var eventSink: EventSink? = null

    @VisibleForTesting
    var isoDep: IsoDep? = null

    @VisibleForTesting
    var timeout: Int? = null

    fun sendCommand(command: ByteArray): ByteArray {
        val currentIsoDep = isoDep ?: throw TagConnectionException()

        return try {
            currentIsoDep.transceive(command)
        } catch (e: TagLostException) {
            throw TagConnectionException()
        } catch (e: SecurityException) {
            throw TagConnectionException()
        }
    }

    override fun onListen(
        arguments: Any?,
        events: EventSink?,
    ) {
        eventSink = events
        if (nfcAdapter == null) {
            eventSink?.error(NfcNotSupportedException())
        } else {
            (arguments as Map<*, *>).run { timeout = arguments["timeout"] as Int? }
            nfcAdapter.enableReaderMode(
                activity,
                ::onTagFound,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B,
                null,
            )
        }
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
        nfcAdapter?.disableReaderMode(activity)
        isoDep?.close()
        isoDep = null
        timeout = null
    }

    @VisibleForTesting
    fun onTagFound(tag: Tag?) {
        if (tag == null) return

        isoDep =
            IsoDep.get(tag) ?: run {
                return emitError(IsoDepNotSupportedException())
            }

        isoDep!!
            .runCatching {
                connect()
                this@TagReader.timeout?.let { timeout = it }
                val tagId = tag.id.toHexString()
                emitSuccess(tagId)
            }.onFailure { exception ->
                if (exception is IOException) {
                    emitError(TagConnectionException())
                }
            }
    }

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
