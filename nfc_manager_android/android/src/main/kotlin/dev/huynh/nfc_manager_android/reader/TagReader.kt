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

/**
 * The TagReader handles the processing of NFC tags and manages the configuration for NFC Host Card Emulation (HCE).
 */
class TagReader(
    private val activity: Activity,
    private val nfcAdapter: NfcAdapter?,
) : StreamHandler {
    /**
     * The event sink for communication with Flutter.
     */
    @VisibleForTesting
    var eventSink: EventSink? = null

    /**
     * The IsoDep for the NFC tag for handling command-response pairs such as SELECT and VERIFY.
     */
    @VisibleForTesting
    var isoDep: IsoDep? = null

    /**
     * The timeout for the NFC tag.
     */
    @VisibleForTesting
    var timeout: Int? = null

    /**
     * Sends a command to the NFC tag.
     * 
     * @param command The command to send.
     * @return The response from the NFC tag.
     */
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

    /**
     * Handles the NFC tag found event.
     * 
     * @param tag The NFC tag found.
     */
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

    /**
     * Emits a success status to the Flutter side.
     * 
     * @param data The data to emit.
     */
    private fun emitSuccess(data: Any) {
        activity.runOnUiThread {
            eventSink?.success(data)
        }
    }

    /**
     * Emits an error status to the Flutter side.
     * 
     * @param exception The NfcException to emit.
     */
    private fun emitError(exception: NfcException) =
        activity.runOnUiThread {
            eventSink?.error(exception)
        }
}
