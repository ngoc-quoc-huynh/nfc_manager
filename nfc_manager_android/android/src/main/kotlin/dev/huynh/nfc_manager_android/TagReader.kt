package dev.huynh.nfc_manager_android

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.annotation.VisibleForTesting
import dev.huynh.nfc_manager_android.models.CommandApdu
import dev.huynh.nfc_manager_android.utils.error
import dev.huynh.nfc_manager_android.utils.toHexString
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Discovers ISO-DEP tags and manages the active tag connection.
 */
internal class TagReader(
    private val activity: Activity,
    private val nfcAdapter: NfcAdapter?,
) : StreamHandler {
    private val stateLock = Any()
    private val ioExecutor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "nfc-tag-io").apply { isDaemon = true }
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var eventSink: EventSink? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var isoDep: IsoDep? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var timeout: Int? = null

    // Bumped on every listen and cancel so the I/O worker can discard stale results.
    private var session = 0L
    private var isListening = false

    /**
     * Sends a command to the active tag and replies on the main thread.
     *
     * A command exceeding Android's transceive limit is answered with [InvalidApduCommandException] and
     * the tag stays connected.
     *
     * @throws NfcException if the command is invalid or no tag is connected.
     */
    fun sendCommand(
        command: ByteArray,
        onSuccess: (ByteArray) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        CommandApdu.fromByteArray(command)

        val (tag, commandSession) =
            synchronized(stateLock) {
                val tag = isoDep ?: throw TagConnectionException()
                tag to session
            }

        try {
            ioExecutor.execute {
                sendCommandOnWorker(tag, commandSession, command, onSuccess, onError)
            }
        } catch (_: RejectedExecutionException) {
            onError(TagConnectionException())
        }
    }

    private fun sendCommandOnWorker(
        tag: IsoDep,
        commandSession: Long,
        command: ByteArray,
        onSuccess: (ByteArray) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        if (!isActive(tag, commandSession)) {
            activity.runOnUiThread { onError(TagConnectionException()) }
            return
        }

        val response =
            try {
                // Android fails an oversized transceive with an IOException, which would
                // drop the tag, so reject it here. A length of 0 is unknown; let the tag decide.
                val maxLength = tag.maxTransceiveLength
                if (maxLength > 0 && command.size > maxLength) null else tag.transceive(command)
            } catch (exception: Exception) {
                val connectionLost =
                    exception is IOException ||
                        exception is SecurityException ||
                        !isActive(tag, commandSession)
                val error =
                    if (connectionLost) {
                        discard(tag)
                        TagConnectionException()
                    } else {
                        exception
                    }
                activity.runOnUiThread { onError(error) }
                return
            }

        if (response == null) {
            activity.runOnUiThread { onError(InvalidApduCommandException()) }
        } else {
            activity.runOnUiThread { onSuccess(response) }
        }
    }

    override fun onListen(
        arguments: Any?,
        events: EventSink?,
    ) {
        val requestedTimeout =
            try {
                parseTimeout(arguments)
            } catch (exception: InvalidArgumentsException) {
                events?.error(exception)
                events?.endOfStream()
                return
            }
        val adapter = nfcAdapter
        if (adapter == null) {
            events?.error(NfcNotSupportedException())
            events?.endOfStream()
            return
        }

        cancelSession()
        val listenSession =
            synchronized(stateLock) {
                if (ioExecutor.isShutdown) return
                session += 1
                isListening = true
                eventSink = events
                timeout = requestedTimeout
                session
            }

        try {
            adapter.enableReaderMode(
                activity,
                { tag -> handleTagFound(tag, listenSession) },
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null,
            )
        } catch (_: Exception) {
            // cancelSession clears eventSink, so report through the sink this call received.
            cancelSession(listenSession)
            events?.error(UnexpectedNfcException())
            events?.endOfStream()
        }
    }

    override fun onCancel(arguments: Any?) {
        cancelSession()
    }

    /**
     * Releases the reader and worker when the plugin detaches from its Activity or engine.
     *
     * An active discovery stream is ended so the Dart listener is not left waiting for tags.
     * Call on the main thread.
     */
    fun dispose() {
        ioExecutor.shutdown()
        val activeSink = synchronized(stateLock) { eventSink }
        cancelSession()
        activeSink?.endOfStream()
    }

    private fun handleTagFound(
        tag: Tag?,
        listenSession: Long,
    ) {
        if (tag == null || !isCurrentSession(listenSession)) return

        val tagIsoDep =
            try {
                IsoDep.get(tag)
            } catch (_: SecurityException) {
                emitError(listenSession, TagConnectionException())
                return
            }
        if (tagIsoDep == null) {
            emitError(listenSession, IsoDepNotSupportedException())
            return
        }

        try {
            ioExecutor.execute { connect(tag, tagIsoDep, listenSession) }
        } catch (_: RejectedExecutionException) {
            emitUnexpectedError(listenSession)
        }
    }

    /** Connects a newly discovered tag and makes it the active one, replacing any previous tag. */
    private fun connect(
        tag: Tag,
        tagIsoDep: IsoDep,
        listenSession: Long,
    ) {
        val requestedTimeout =
            synchronized(stateLock) {
                if (!isCurrentSession(listenSession)) return
                timeout
            }

        try {
            tagIsoDep.connect()
            requestedTimeout?.let { tagIsoDep.timeout = it }
            val tagId = tag.id.toHexString()

            // Decide under the lock, but close outside it: close() is blocking I/O.
            val (accepted, previous) =
                synchronized(stateLock) {
                    if (isCurrentSession(listenSession)) {
                        true to isoDep.also { isoDep = tagIsoDep }
                    } else {
                        false to null
                    }
                }
            if (!accepted) {
                closeQuietly(tagIsoDep)
                return
            }
            previous?.let(::closeQuietly)
            emitSuccess(listenSession, tagId)
        } catch (exception: Exception) {
            closeQuietly(tagIsoDep)
            emitError(
                listenSession,
                if (exception is IOException || exception is SecurityException) {
                    TagConnectionException()
                } else {
                    UnexpectedNfcException()
                },
            )
        }
    }

    private fun cancelSession(expectedSession: Long? = null) {
        val (wasListening, activeIsoDep) =
            synchronized(stateLock) {
                if (expectedSession != null && session != expectedSession) return

                val state = isListening to isoDep
                session += 1
                isListening = false
                eventSink = null
                timeout = null
                isoDep = null
                state
            }

        // Closing from this thread also unblocks a transceive in progress on the worker.
        activeIsoDep?.let(::closeQuietly)
        if (wasListening) releaseReaderMode()
    }

    private fun releaseReaderMode() {
        try {
            nfcAdapter?.disableReaderMode(activity)
        } catch (_: RuntimeException) {
            // Throws without the NFC feature or for a destroyed activity. Android already
            // releases reader mode when the activity pauses and drops it when it is destroyed.
        }
    }

    private fun isCurrentSession(listenSession: Long): Boolean =
        synchronized(stateLock) { isListening && session == listenSession }

    private fun isActive(
        tag: IsoDep,
        commandSession: Long,
    ): Boolean = synchronized(stateLock) { isoDep === tag && session == commandSession }

    /** Forgets and closes [tag] if it is still the active tag. */
    private fun discard(tag: IsoDep) {
        val wasActive =
            synchronized(stateLock) {
                (isoDep === tag).also { if (it) isoDep = null }
            }
        if (wasActive) closeQuietly(tag)
    }

    private fun closeQuietly(tag: IsoDep) {
        try {
            tag.close()
        } catch (_: Exception) {
            // The tag may be out of range or already closed.
        }
    }

    /**
     * Reads the optional positive `timeout` in milliseconds from the discovery arguments.
     *
     * @throws InvalidArgumentsException if the arguments are not a map or the timeout is not a
     * positive integer.
     */
    private fun parseTimeout(arguments: Any?): Int? {
        val options =
            when (arguments) {
                null -> emptyMap<Any?, Any?>()
                is Map<*, *> -> arguments
                else -> throw InvalidArgumentsException()
            }
        val value = options["timeout"] ?: return null
        val requestedTimeout = value as? Int ?: throw InvalidArgumentsException()
        if (requestedTimeout <= 0) throw InvalidArgumentsException()
        return requestedTimeout
    }

    /** Delivers to the sink on the main thread, dropping events from stale sessions. */
    private fun emit(
        listenSession: Long,
        action: (EventSink) -> Unit,
    ) {
        activity.runOnUiThread {
            val sink = synchronized(stateLock) { if (isListening && session == listenSession) eventSink else null }
            sink?.let(action)
        }
    }

    private fun emitSuccess(
        listenSession: Long,
        tagId: String,
    ) = emit(listenSession) { it.success(tagId) }

    private fun emitError(
        listenSession: Long,
        exception: NfcException,
    ) = emit(listenSession) { it.error(exception) }

    private fun emitUnexpectedError(listenSession: Long) =
        emitError(listenSession, UnexpectedNfcException())
}
