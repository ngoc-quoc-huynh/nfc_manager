package dev.huynh.nfc_manager_android.models

import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.InvalidLcDataLengthException
import dev.huynh.nfc_manager_android.utils.toUnsignedInt

/**
 * Application Protocol Data Unit (APDU) commands used in NFC operations.
 */
internal class CommandApdu(
    /**
     * The class byte (CLA) of the APDU command, which identifies the category of
     * the command.
     */
    val cla: Byte,
    /**
     * The instruction byte (INS) of the APDU command, which specifies the operation
     * to be performed.
     */
    val ins: Byte,
    /**
     * The first parameter byte (P1) of the APDU command, used for providing
     * further instruction specifics or conditions.
     */
    val p1: Byte,
    /**
     * The second parameter byte (P2) of the APDU command, used for providing
     * further instruction specifics or conditions.
     */
    val p2: Byte,
    /**
     * Optional data payload for the command, represented as a sequence of bytes.
     * This field is used when the command needs to send data to a card or
     * device.
     */
    val data: ByteArray? = null,
    /**
     * An optional expected length byte (Le) that specifies the maximum number
     * of bytes expected in the response from the card.
     * This is not required if no response is expected.
     */
    val le: Byte? = null,
) {
    companion object {
        /**
         * Creates a [CommandApdu] from a byte array.
         *
         * Only short APDUs (ISO/IEC 7816-4) are supported: Lc and Le are single bytes.
         * Extended-length APDUs, whose Lc and Le fields span multiple bytes, are rejected.
         *
         * @param command the byte array representing the APDU command.
         * @return the [CommandApdu] object.
         * @throws InvalidApduCommandException if the command is invalid.
         * @throws InvalidLcDataLengthException if the Lc data length is invalid, which
         *   includes extended-length commands.
         */
        fun fromByteArray(command: ByteArray): CommandApdu {
            if (command.size < 4) {
                throw InvalidApduCommandException()
            }

            val cla = command[0]
            val ins = command[1]
            val p1 = command[2]
            val p2 = command[3]

            var data: ByteArray? = null
            var le: Byte? = null

            if (command.size == 5) {
                le = command[4]
            } else if (command.size > 5) {
                // ISO/IEC 7816-4 short APDUs disallow Lc = 00.
                val lcLength = command[4].toUnsignedInt()
                val dataEndIndex = lcLength + 5
                val isLePresent = command.size == dataEndIndex + 1

                if (lcLength == 0 || (command.size != dataEndIndex && !isLePresent)) {
                    throw InvalidLcDataLengthException()
                }

                data = command.sliceArray(5 until dataEndIndex)

                if (isLePresent) {
                    le = command[dataEndIndex]
                }
            }

            return CommandApdu(
                cla = cla,
                ins = ins,
                p1 = p1,
                p2 = p2,
                data = data,
                le = le,
            )
        }
    }
}
