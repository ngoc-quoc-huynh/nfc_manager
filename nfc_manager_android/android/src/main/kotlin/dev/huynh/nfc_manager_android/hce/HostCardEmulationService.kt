package dev.huynh.nfc_manager_android.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import dev.huynh.nfc_manager_android.CommandApdu
import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.InvalidLcDataLengthException
import dev.huynh.nfc_manager_android.PinBlockedException
import dev.huynh.nfc_manager_android.hce.HostCardEmulationSession.PinVerificationResult

/**
 * The HCE service that apps declare in their manifest; Android creates and binds it.
 *
 * It answers SELECT AID and VERIFY APDUs against the active [HostCardEmulationSession]
 * and emits each outcome to the session's sink:
 * * No session: `69 82`. No command: `6A 00`. Neither emits an event.
 * * VERIFY before the configured AID is selected on the current link: `69 85`, no event, and it
 *   does not count as a wrong PIN.
 * * A wrong PIN: `63 CX` with X attempts left. The third in a row answers `69 83` and ends the
 *   session.
 * * Another instruction: `6D 00`. SELECT or VERIFY with another P1: `6A 81`.
 *
 * Android calls it on the main thread, which is also where the session changes.
 */
class HostCardEmulationService : HostApduService() {
    /** The session that answered a command on the current link, or null. */
    private var sessionUsedForCurrentLink: HostCardEmulationSession? = null

    /** The session whose AID the reader selected on the current link, or null. */
    private var selectedSession: HostCardEmulationSession? = null

    override fun processCommandApdu(
        commandByteArray: ByteArray?,
        extras: Bundle?,
    ): ByteArray {
        val session = HostCardEmulationSessionHolder.active
        if (session != null && commandByteArray != null) {
            sessionUsedForCurrentLink = session
        }
        val response =
            when {
                session == null -> ResponseApdu.HCE_NOT_READY
                commandByteArray == null -> ResponseApdu.NULL_COMMAND
                else -> handleCommand(session, commandByteArray)
            }
        return response.toByteArray()
    }

    override fun onDeactivated(reason: Int) {
        val session = sessionUsedForCurrentLink
        sessionUsedForCurrentLink = null
        selectedSession = null
        if (session != null && HostCardEmulationSessionHolder.active === session) {
            session.emit(HostCardEmulationStatus.TAG_DISCONNECTED)
        }
    }

    private fun handleCommand(
        session: HostCardEmulationSession,
        commandByteArray: ByteArray,
    ): ResponseApdu =
        try {
            val command = CommandApdu.fromByteArray(commandByteArray)
            when {
                command.ins == INS_SELECT && command.p1 == P1_SELECT_BY_NAME -> {
                    selectAid(session, command.data ?: throw InvalidLcDataLengthException())
                }

                command.ins == INS_VERIFY && command.p1 == P1_VERIFY -> {
                    val pin = command.data ?: throw InvalidLcDataLengthException()
                    if (selectedSession === session) verifyPin(session, pin) else ResponseApdu.AID_NOT_SELECTED
                }

                else -> {
                    session.emit(HostCardEmulationStatus.FUNCTION_NOT_SUPPORTED)
                    if (command.ins == INS_SELECT || command.ins == INS_VERIFY) {
                        ResponseApdu.FUNCTION_NOT_SUPPORTED
                    } else {
                        ResponseApdu.INSTRUCTION_NOT_SUPPORTED
                    }
                }
            }
        } catch (_: InvalidApduCommandException) {
            session.emit(HostCardEmulationStatus.INVALID_COMMAND)
            ResponseApdu.WRONG_LENGTH
        } catch (_: InvalidLcDataLengthException) {
            session.emit(HostCardEmulationStatus.INVALID_LC_LENGTH)
            ResponseApdu.WRONG_LENGTH
        }

    private fun selectAid(
        session: HostCardEmulationSession,
        aid: ByteArray,
    ): ResponseApdu =
        if (session.matchesAid(aid)) {
            selectedSession = session
            session.emit(HostCardEmulationStatus.AID_SELECTED)
            ResponseApdu.OK
        } else {
            selectedSession = null
            session.emit(HostCardEmulationStatus.INVALID_AID)
            ResponseApdu.INVALID_AID
        }

    private fun verifyPin(
        session: HostCardEmulationSession,
        pin: ByteArray,
    ): ResponseApdu =
        when (session.verifyPin(pin)) {
            PinVerificationResult.VERIFIED -> {
                session.emit(HostCardEmulationStatus.PIN_VERIFIED)
                ResponseApdu.OK
            }

            PinVerificationResult.WRONG_PIN -> {
                session.emit(HostCardEmulationStatus.WRONG_PIN)
                ResponseApdu.wrongPin(session.pinAttemptsLeft)
            }

            PinVerificationResult.BLOCKED -> {
                HostCardEmulationSessionHolder.end(session, PinBlockedException())
                ResponseApdu.PIN_BLOCKED
            }
        }

    private companion object {
        const val INS_SELECT: Byte = 0xA4.toByte()
        const val P1_SELECT_BY_NAME: Byte = 0x04
        const val INS_VERIFY: Byte = 0x20
        const val P1_VERIFY: Byte = 0x00
    }
}
