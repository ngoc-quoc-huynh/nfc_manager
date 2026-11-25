package dev.huynh.nfc_manager_android.hce

import dev.huynh.nfc_manager_android.NfcException
import io.flutter.plugin.common.EventChannel.StreamHandler

/**
 * Holds the active HCE session.
 *
 * Android creates the [HostCardEmulationService] and the plugin creates the
 * [HostCardEmulationStreamHandler], so they share the session through this object. Both use it
 * only on the main thread, so it needs no locking.
 */
internal object HostCardEmulationSessionHolder {
    /** The active session, or null when HCE is not started. */
    var active: HostCardEmulationSession? = null
        private set

    /** Replaces the active session with [session]. */
    fun start(session: HostCardEmulationSession) {
        active = session
    }

    /** Clears the active session if [owner] started it. */
    fun clear(owner: StreamHandler) {
        if (active?.owner === owner) active = null
    }

    /** Clears [session] if it is still active, then reports [exception] and ends its stream. */
    fun end(
        session: HostCardEmulationSession,
        exception: NfcException,
    ) {
        if (active !== session) return
        active = null
        session.fail(exception)
    }
}
