package dev.huynh.nfc_manager_android.host_card_emulation

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.NfcException
import dev.huynh.nfc_manager_android.models.CommandApdu
import dev.huynh.nfc_manager_android.models.ResponseApdu
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink

class HostCardEmulation :
    HostApduService(),
    EventChannel.StreamHandler {
    private var eventSink: EventSink? = null

    fun startEmulation(
        aid: ByteArray,
        pin: ByteArray,
    ) {
        HostCardEmulationConfig.configure(
            aid = aid,
            pin = pin,
        )
        eventSink?.success(HostCardEmulationStatus.READY)
    }

    fun stopEmulation() {
        HostCardEmulationConfig.clear()
        eventSink?.success(HostCardEmulationStatus.DEACTIVATED)
    }

    override fun processCommandApdu(
        commandByteArray: ByteArray?,
        extras: Bundle?,
    ): ByteArray =
        when {
            !HostCardEmulationConfig.isConfigured -> {
                eventSink?.success(HostCardEmulationStatus.NOT_CONFIGURED)
                ResponseApdu.HCE_NOT_READY()
            }

            commandByteArray == null -> ResponseApdu.NULL_COMMAND()
            else -> handleCommand(commandByteArray)
        }

    private fun handleCommand(commandByteArray: ByteArray): ByteArray =
        try {
            val command = CommandApdu.fromByteArray(commandByteArray)

            when {
                command.ins == 0xA4.toByte() && command.p1 == 0x04.toByte() ->
                    processSelectAid(command.data!!)

                command.ins == 0x20.toByte() && command.p1 == 0x00.toByte() ->
                    processVerifyPin(command.data!!)

                else -> {
                    eventSink?.success(HostCardEmulationStatus.FUNCTION_NOT_SUPPORTED)
                    ResponseApdu.FUNCTION_NOT_SUPPORTED
                }
            }()
        } catch (e: InvalidApduCommandException) {
            ResponseApdu.WRONG_LENGTH()
        } catch (e: NfcException) {
            ResponseApdu.UNKNOWN()
        }

    private fun processSelectAid(data: ByteArray): ResponseApdu =
        when {
            !HostCardEmulationConfig.isConfigured -> {
                eventSink?.success(HostCardEmulationStatus.NOT_CONFIGURED)
                ResponseApdu.HCE_NOT_READY
            }

            data.contentEquals(HostCardEmulationConfig.aid) -> {
                eventSink?.success(HostCardEmulationStatus.AID_SELECTED)
                ResponseApdu.OK
            }

            else -> {
                eventSink?.success(HostCardEmulationStatus.INVALID_AID)
                ResponseApdu.INVALID_AID
            }
        }

    private fun processVerifyPin(data: ByteArray): ResponseApdu =
        when {
            !HostCardEmulationConfig.isConfigured -> {
                eventSink?.success(HostCardEmulationStatus.NOT_CONFIGURED)
                ResponseApdu.HCE_NOT_READY
            }

            data.contentEquals(HostCardEmulationConfig.pin!!) -> {
                eventSink?.success(HostCardEmulationStatus.PIN_VERIFIED)
                ResponseApdu.OK
            }

            else -> {
                eventSink?.success(HostCardEmulationStatus.WRONG_PIN)
                ResponseApdu.WRONG_PIN
            }
        }

    override fun onDeactivated(reason: Int) {
        eventSink?.success(HostCardEmulationStatus.TAG_DISCONNECTED)
    }

    override fun onListen(
        arguments: Any?,
        events: EventSink?,
    ) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }
}
