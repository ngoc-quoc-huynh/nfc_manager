package dev.huynh.nfc_manager_android.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.InvalidLcDataLengthException
import dev.huynh.nfc_manager_android.models.CommandApdu
import dev.huynh.nfc_manager_android.models.HostCardEmulationStatus
import dev.huynh.nfc_manager_android.models.ResponseApdu
import java.security.MessageDigest

/**
 * The HCE service that apps declare in their manifest; Android creates and binds it.
 *
 * It answers SELECT AID and VERIFY PIN APDUs from an NFC reader against the active
 * [HostCardEmulationConfig.Session] and emits each outcome to the session's event sink. Without a
 * session it answers `69 82` (HCE not ready).
 */
class HostCardEmulationService : HostApduService() {
    override fun processCommandApdu(
        commandByteArray: ByteArray?,
        extras: Bundle?,
    ): ByteArray {
        val session = HostCardEmulationConfig.session
        val response =
            when {
                session == null -> ResponseApdu.HCE_NOT_READY
                commandByteArray == null -> ResponseApdu.NULL_COMMAND
                else -> handleCommand(session, commandByteArray)
            }
        return response.toByteArray()
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
    ): ResponseApdu =
        try {
            val command = CommandApdu.fromByteArray(commandByteArray)

            when {
                command.ins == 0xA4.toByte() && command.p1 == 0x04.toByte() -> {
                    processSelectAid(session, command.data ?: throw InvalidLcDataLengthException())
                }

                command.ins == 0x20.toByte() && command.p1 == 0x00.toByte() -> {
                    processVerifyPin(session, command.data ?: throw InvalidLcDataLengthException())
                }

                else -> {
                    session.emit(HostCardEmulationStatus.FUNCTION_NOT_SUPPORTED)
                    ResponseApdu.FUNCTION_NOT_SUPPORTED
                }
            }
        } catch (e: InvalidApduCommandException) {
            session.emit(HostCardEmulationStatus.INVALID_COMMAND)
            ResponseApdu.WRONG_LENGTH
        } catch (e: InvalidLcDataLengthException) {
            session.emit(HostCardEmulationStatus.INVALID_LC_LENGTH)
            ResponseApdu.WRONG_LENGTH
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
}
