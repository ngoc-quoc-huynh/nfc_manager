package dev.huynh.nfc_manager_android.host_card_emulation

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.NfcException
import dev.huynh.nfc_manager_android.models.CommandApdu
import dev.huynh.nfc_manager_android.models.ResponseApdu

class HostCardEmulation : HostApduService() {
    fun startEmulation(
        aid: ByteArray,
        pin: ByteArray,
    ) = HostCardEmulationConfig.configure(
        aid = aid,
        pin = pin,
    )

    fun stopEmulation() = HostCardEmulationConfig.clear()

    override fun processCommandApdu(
        commandApdu: ByteArray?,
        extras: Bundle?,
    ): ByteArray =
        when {
            !HostCardEmulationConfig.isConfigured -> ResponseApdu.HCE_NOT_READY()
            commandApdu == null -> ResponseApdu.NULL_COMMAND()
            else -> handleCommand(commandApdu)
        }

    private fun handleCommand(commandApdu: ByteArray): ByteArray =
        try {
            val parsedCommand = CommandApdu.fromByteArray(commandApdu)

            when {
                parsedCommand.ins == 0xA4.toByte() && parsedCommand.p1 == 0x04.toByte() ->
                    processSelectAid(parsedCommand.data!!)

                parsedCommand.ins == 0x20.toByte() && parsedCommand.p1 == 0x00.toByte() ->
                    processVerifyPin(parsedCommand.data!!)

                else -> ResponseApdu.FUNCTION_NOT_SUPPORTED
            }()
        } catch (e: InvalidApduCommandException) {
            ResponseApdu.WRONG_LENGTH()
        } catch (e: NfcException) {
            ResponseApdu.UNKNOWN()
        }

    private fun processSelectAid(data: ByteArray): ResponseApdu =
        when {
            HostCardEmulationConfig.aid == null -> ResponseApdu.HCE_NOT_READY
            data.contentEquals(HostCardEmulationConfig.aid) -> ResponseApdu.OK
            else -> ResponseApdu.INVALID_AID
        }

    private fun processVerifyPin(data: ByteArray): ResponseApdu =
        when {
            HostCardEmulationConfig.pin == null -> ResponseApdu.HCE_NOT_READY
            data.contentEquals(HostCardEmulationConfig.pin!!) -> ResponseApdu.OK
            else -> ResponseApdu.WRONG_PIN
        }

    override fun onDeactivated(reason: Int) = Unit
}
