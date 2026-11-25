package dev.huynh.nfc_manager_android.reader

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import androidx.annotation.VisibleForTesting
import dev.huynh.nfc_manager_android.CommandApdu
import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.InvalidArgumentsException
import dev.huynh.nfc_manager_android.InvalidLcDataLengthException
import dev.huynh.nfc_manager_android.IsoDepNotSupportedException
import dev.huynh.nfc_manager_android.NfcException
import dev.huynh.nfc_manager_android.NfcNotSupportedException
import dev.huynh.nfc_manager_android.TagConnectionException
import dev.huynh.nfc_manager_android.UnexpectedNfcException
import dev.huynh.nfc_manager_android.endWithError
import dev.huynh.nfc_manager_android.error
import dev.huynh.nfc_manager_android.toHex
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * Discovers ISO-DEP tags and manages the active tag connection.
 *
 * @param ioExecutor is dedicated to this reader and runs blocking tag I/O one task at a time,
 * in submission order, on a background thread. [dispose] shuts it down.
 * @param mainThreadExecutor delivers asynchronous command replies and discovery events on the
 * main thread. It must accept pending callbacks through [dispose].
 * @param isoDepFor returns the ISO-DEP technology of a discovered tag, or null if it has none.
 */
internal class TagReader(
    private val activity: Activity,
    private val nfcAdapter: NfcAdapter?,
    private val ioExecutor: ExecutorService = newTagIoExecutor(),
    private val mainThreadExecutor: Executor = Executor(activity::runOnUiThread),
    private val isoDepFor: (Tag) -> IsoDep? = IsoDep::get,
) : StreamHandler {
    private val stateLock = Any()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var eventSink: EventSink? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var isoDep: IsoDep? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var timeout: Int? = null

    // Bumped on every listen and cancel so the I/O worker can discard stale results.
    private var session = 0L
    private var isListening = false

    /**
     * Sends [command] to the active tag and replies on the main thread.
     *
     * A malformed command, or one longer than Android's transceive limit, is answered with
     * [InvalidApduCommandException], and the tag stays connected. Without a tag, it is answered
     * with [TagConnectionException].
     */
    fun sendCommand(
        command: ByteArray,
        onSuccess: (ByteArray) -> Unit,
        onError: (NfcException) -> Unit,
    ) {
        try {
            CommandApdu.fromByteArray(command)
        } catch (exception: InvalidApduCommandException) {
            return onError(exception)
        } catch (_: InvalidLcDataLengthException) {
            return onError(InvalidApduCommandException())
        }

        val (tag, commandSession) =
            synchronized(stateLock) { isoDep?.let { it to session } }
                ?: return onError(TagConnectionException())

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
        onError: (NfcException) -> Unit,
    ) = try {
        val response = transceive(tag, commandSession, command)
        mainThreadExecutor.execute { onSuccess(response) }
    } catch (exception: NfcException) {
        mainThreadExecutor.execute { onError(exception) }
    }

    /**
     * @throws InvalidApduCommandException if the command exceeds the transceive limit.
     * @throws TagConnectionException if the tag is no longer active or the connection failed.
     * @throws UnexpectedNfcException if the transceive failed for another reason.
     */
    private fun transceive(
        tag: IsoDep,
        commandSession: Long,
        command: ByteArray,
    ): ByteArray {
        if (!isActive(tag, commandSession)) throw TagConnectionException()
        // Android fails an oversized transceive with an IOException, which looks like a lost
        // tag, so reject it here. A length of 0 is unknown; let the tag decide.
        val maxLength = callTag(tag, commandSession) { tag.maxTransceiveLength }
        if (maxLength in 1 until command.size) throw InvalidApduCommandException()
        return callTag(tag, commandSession) { tag.transceive(command) }
    }

    /**
     * Runs [call] on [tag]. An I/O or security failure, or one caused by cancellation, discards the
     * tag and throws [TagConnectionException]; any other failure throws [UnexpectedNfcException].
     */
    private inline fun <T> callTag(
        tag: IsoDep,
        commandSession: Long,
        call: () -> T,
    ): T =
        try {
            call()
        } catch (exception: Exception) {
            if (exception !is IOException && exception !is SecurityException && isActive(tag, commandSession)) {
                throw UnexpectedNfcException()
            }
            discard(tag)
            throw TagConnectionException()
        }

    override fun onListen(
        arguments: Any?,
        events: EventSink,
    ) {
        val requestedTimeout =
            try {
                parseTimeout(arguments)
            } catch (exception: InvalidArgumentsException) {
                events.endWithError(exception)
                return
            }
        val adapter = nfcAdapter
        if (adapter == null) {
            events.endWithError(NfcNotSupportedException())
            return
        }

        cancelSession()
        val listenSession =
            synchronized(stateLock) {
                if (ioExecutor.isShutdown) return@synchronized null
                session += 1
                isListening = true
                eventSink = events
                timeout = requestedTimeout
                session
            }
        if (listenSession == null) {
            // dispose already shut down the worker.
            events.endWithError(UnexpectedNfcException())
            return
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
            events.endWithError(UnexpectedNfcException())
        }
    }

    override fun onCancel(arguments: Any?) {
        cancelSession()
    }

    /**
     * Ends an active discovery stream and releases the reader and its worker. Call on the main
     * thread when the plugin detaches from its activity or engine.
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
                isoDepFor(tag)
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
            emitError(listenSession, UnexpectedNfcException())
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
            val tagId = tag.id.toHex()

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

    private fun isCurrentSession(listenSession: Long): Boolean = synchronized(stateLock) { isListening && session == listenSession }

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
     * positive `Int`.
     */
    private fun parseTimeout(arguments: Any?): Int? {
        if (arguments != null && arguments !is Map<*, *>) throw InvalidArgumentsException()
        val value = arguments?.get("timeout") ?: return null
        return (value as? Int)?.takeIf { it > 0 } ?: throw InvalidArgumentsException()
    }

    /** Delivers to the sink on the main thread, dropping events from stale sessions. */
    private fun emit(
        listenSession: Long,
        action: (EventSink) -> Unit,
    ) {
        mainThreadExecutor.execute {
            val sink = synchronized(stateLock) { eventSink.takeIf { isCurrentSession(listenSession) } }
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

    private companion object {
        fun newTagIoExecutor(): ExecutorService =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "nfc-tag-io").apply { isDaemon = true }
            }
    }
}
