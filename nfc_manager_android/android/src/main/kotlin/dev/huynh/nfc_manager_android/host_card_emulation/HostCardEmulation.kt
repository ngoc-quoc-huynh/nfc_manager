package dev.huynh.nfc_manager_android.host_card_emulation

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.InvalidLcDataLengthException
import dev.huynh.nfc_manager_android.models.CommandApdu
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.models.ResponseApdu
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import java.security.MessageDigest

/**
 * The HostCardEmulation is responsible for handling APDU (Application Protocol Data Unit) commands
 * received from NFC terminals.
 *
 * It manages the NFC Host Card Emulation (HCE) configuration and processes commands based on the
 * current configuration.
 * 
 * The class also interfaces with a Flutter application via EventChannel, allowing it to send
 * status updates and responses back to the Flutter side.
 *
 * This integration facilitates the dynamic handling of NFC operations and event-driven communication.
 */
class HostCardEmulation :
    HostApduService(),
    StreamHandler {
    override fun processCommandApdu(
        commandByteArray: ByteArray?,
        extras: Bundle?,
    ): ByteArray {
        val session = HostCardEmulationConfig.session ?: return ResponseApdu.HCE_NOT_READY()
        return when (commandByteArray) {
            null -> ResponseApdu.NULL_COMMAND()
            else -> handleCommand(session, commandByteArray)
        }
    }

    /**
     * Handles the APDU command received from the NFC terminal.
     * 
     * @param session The session snapshot used for the whole command.
     * @param commandByteArray The APDU command.
     * @return The response APDU.
     */
    private fun handleCommand(
        session: HostCardEmulationConfig.Session,
        commandByteArray: ByteArray,
    ): ByteArray =
        try {
            val command = CommandApdu.fromByteArray(commandByteArray)

            when {
                command.ins == 0xA4.toByte() && command.p1 == 0x04.toByte() ->
                    processSelectAid(session, command.data ?: throw InvalidLcDataLengthException())

                command.ins == 0x20.toByte() && command.p1 == 0x00.toByte() ->
                    processVerifyPin(session, command.data ?: throw InvalidLcDataLengthException())

                else -> {
                    session.emit(HostCardEmulationStatus.FUNCTION_NOT_SUPPORTED)
                    ResponseApdu.FUNCTION_NOT_SUPPORTED
                }
            }()
        } catch (e: InvalidApduCommandException) {
            session.emit(HostCardEmulationStatus.INVALID_COMMAND)
            ResponseApdu.WRONG_LENGTH()
        } catch (e: InvalidLcDataLengthException) {
            session.emit(HostCardEmulationStatus.INVALID_LC_LENGTH)
            ResponseApdu.WRONG_LC_LENGTH()
        }

    /**
     * Processes the SELECT AID command.
     * 
     * @param session The session snapshot used for the whole command.
     * @param data The data of the SELECT AID command.
     * @return The response APDU.
     */
    private fun processSelectAid(
        session: HostCardEmulationConfig.Session,
        data: ByteArray,
    ): ResponseApdu =
        if (data.contentEquals(session.aid)) {
            session.emit(HostCardEmulationStatus.AID_SELECTED)
            ResponseApdu.OK
        } else {
            session.emit(HostCardEmulationStatus.INVALID_AID)
            ResponseApdu.INVALID_AID
        }

    /**
     * Processes the VERIFY PIN command.
     *
     * Compares equal-length PINs across all bytes, even after a mismatch.
     * 
     * @param session The session snapshot used for the whole command.
     * @param data The data of the VERIFY PIN command.
     * @return The response APDU.
     */
    private fun processVerifyPin(
        session: HostCardEmulationConfig.Session,
        data: ByteArray,
    ): ResponseApdu =
        if (MessageDigest.isEqual(data, session.pin)) {
            session.emit(HostCardEmulationStatus.PIN_VERIFIED)
            ResponseApdu.OK
        } else {
            session.emit(HostCardEmulationStatus.WRONG_PIN)
            ResponseApdu.WRONG_PIN
        }

    override fun onDeactivated(reason: Int) {
        HostCardEmulationConfig.session?.emit(HostCardEmulationStatus.TAG_DISCONNECTED)
    }

    private var closed = false

    override fun onListen(
        arguments: Any?,
        events: EventSink,
    ) {
        check(!closed) { "The HCE stream handler is detached from the engine." }
        (arguments as Map<*, *>).run {
            val session =
                HostCardEmulationConfig.Session(
                    owner = this@HostCardEmulation,
                    aid = arguments["aid"] as ByteArray,
                    pin = arguments["pin"] as ByteArray,
                    sink = events,
                )
            HostCardEmulationConfig.start(session)
            session.emit(HostCardEmulationStatus.READY)
        }
    }

    override fun onCancel(arguments: Any?) = clearSession()

    internal fun close() {
        closed = true
        clearSession()
    }

    private fun clearSession() = HostCardEmulationConfig.clear(owner = this)

    /**
     * Emits a success status to the Flutter side.
     * 
     * @param status The status to emit.
     */
    private fun HostCardEmulationConfig.Session.emit(status: HostCardEmulationStatus) = sink.success(status.name)
}
