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
    ): ByteArray =
        when {
            !HostCardEmulationConfig.isConfigured -> {
                ResponseApdu.HCE_NOT_READY()
            }

            commandByteArray == null -> ResponseApdu.NULL_COMMAND()
            else -> handleCommand(commandByteArray)
        }

    /**
     * Handles the APDU command received from the NFC terminal.
     * 
     * @param commandByteArray The APDU command.
     * @return The response APDU.
     */
    private fun handleCommand(commandByteArray: ByteArray): ByteArray =
        try {
            val command = CommandApdu.fromByteArray(commandByteArray)

            when {
                command.ins == 0xA4.toByte() && command.p1 == 0x04.toByte() ->
                    processSelectAid(command.data!!)

                command.ins == 0x20.toByte() && command.p1 == 0x00.toByte() ->
                    processVerifyPin(command.data!!)

                else -> {
                    emitSuccess(HostCardEmulationStatus.FUNCTION_NOT_SUPPORTED)
                    ResponseApdu.FUNCTION_NOT_SUPPORTED
                }
            }()
        } catch (e: InvalidApduCommandException) {
            emitSuccess(HostCardEmulationStatus.INVALID_COMMAND)
            ResponseApdu.WRONG_LENGTH()
        } catch (e: InvalidLcDataLengthException) {
            emitSuccess(HostCardEmulationStatus.INVALID_LC_LENGTH)
            ResponseApdu.WRONG_LC_LENGTH()
        }

    /**
     * Processes the SELECT AID command.
     * 
     * @param data The data of the SELECT AID command.
     * @return The response APDU.
     */
    private fun processSelectAid(data: ByteArray): ResponseApdu =
        if (data.contentEquals(HostCardEmulationConfig.aid)) {
            emitSuccess(HostCardEmulationStatus.AID_SELECTED)
            ResponseApdu.OK
        } else {
            emitSuccess(HostCardEmulationStatus.INVALID_AID)
            ResponseApdu.INVALID_AID
        }

    /**
     * Processes the VERIFY PIN command.
     * 
     * @param data The data of the VERIFY PIN command.
     * @return The response APDU.
     */
    private fun processVerifyPin(data: ByteArray): ResponseApdu =
        if (data.contentEquals(HostCardEmulationConfig.pin!!)) {
            emitSuccess(HostCardEmulationStatus.PIN_VERIFIED)
            ResponseApdu.OK
        } else {
            emitSuccess(HostCardEmulationStatus.WRONG_PIN)
            ResponseApdu.WRONG_PIN
        }

    override fun onDeactivated(reason: Int) {
        emitSuccess(HostCardEmulationStatus.TAG_DISCONNECTED)
    }

    override fun onListen(
        arguments: Any?,
        events: EventSink,
    ): Unit =
        (arguments as Map<*, *>).run {
            HostCardEmulationConfig.configure(
                aid = arguments["aid"] as ByteArray,
                pin = arguments["pin"] as ByteArray,
                eventSink = events,
            )
            emitSuccess(HostCardEmulationStatus.READY)
        }

    override fun onCancel(arguments: Any?) = HostCardEmulationConfig.clear()

    /**
     * Emits a success status to the Flutter side.
     * 
     * @param status The status to emit.
     */
    private fun emitSuccess(status: HostCardEmulationStatus) =
        HostCardEmulationConfig.eventSink
            ?.success(status.name)
}
