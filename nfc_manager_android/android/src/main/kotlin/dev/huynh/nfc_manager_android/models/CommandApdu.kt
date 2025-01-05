package dev.huynh.nfc_manager_android.models

import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.InvalidLcDataLengthException
import dev.huynh.nfc_manager_android.utils.toUnsignedInt
import java.util.Objects

/**
 * Application Protocol Data Unit (APDU) commands used in NFC operations.
 */
data class CommandApdu(
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
     * An optional length byte (Lc) that indicates the number of bytes in the
     * command data field.
     * This is only present if the command includes data.
     */
    val lc: Byte? = null,

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
         * @param command the byte array representing the APDU command.
         * @return the [CommandApdu] object.
         * @throws InvalidApduCommandException if the command is invalid.
         * @throws InvalidLcDataLengthException if the Lc data length is invalid.
         */
        fun fromByteArray(command: ByteArray): CommandApdu {
            if (command.size < 4) {
                throw InvalidApduCommandException()
            }

            val cla = command[0]
            val ins = command[1]
            val p1 = command[2]
            val p2 = command[3]

            var lc: Byte? = null
            var data: ByteArray? = null
            var le: Byte? = null

            when (command.size) {
                4 -> Unit
                5 -> le = command[4]
                6 -> {
                    lc = command[4]
                    val lcLength = lc.toUnsignedInt()

                    when (lcLength) {
                        0 -> le = command[5]
                        1 -> data = command.sliceArray(5 until 6)
                        else -> throw InvalidLcDataLengthException()
                    }
                }

                else -> {
                    lc = command[4]
                    val lcLength = lc.toUnsignedInt()
                    val dataEndIndex = lcLength + 5
                    val isLePresent = command.size == dataEndIndex + 1

                    if (command.size != dataEndIndex && !isLePresent) {
                        throw InvalidLcDataLengthException()
                    }

                    data = command.sliceArray(5 until dataEndIndex)

                    if (isLePresent) {
                        le = command[dataEndIndex]
                    }
                }
            }

            return CommandApdu(
                cla = cla,
                ins = ins,
                p1 = p1,
                p2 = p2,
                lc = lc,
                data = data,
                le = le,
            )
        }
    }

    /**
     * Converts the [CommandApdu] to a byte array.
     *
     * @return the byte array representing the APDU command.
     */
    fun toByteArray(): ByteArray =
        buildList {
            add(cla)
            add(ins)
            add(p1)
            add(p2)
            lc?.let { add(it) }
            data?.let { addAll(it.toList()) }
            le?.let { add(it) }
        }.toByteArray()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as CommandApdu

        return cla == other.cla &&
            ins == other.ins &&
            p1 == other.p1 &&
            p2 == other.p2 &&
            lc == other.lc &&
            data.contentEquals(other.data) &&
            le == other.le
    }

    override fun hashCode(): Int =
        Objects.hash(
            cla,
            ins,
            p1,
            p2,
            lc,
            data?.contentHashCode(),
            le,
        )
}
