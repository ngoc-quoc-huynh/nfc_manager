package dev.huynh.nfc_manager_android.models

import dev.huynh.nfc_manager_android.InvalidApduCommandException
import dev.huynh.nfc_manager_android.InvalidLcDataLengthException
import dev.huynh.nfc_manager_android.utils.toUnsignedInt
import java.util.Objects

data class CommandApdu(
    val cla: Byte,
    val ins: Byte,
    val p1: Byte,
    val p2: Byte,
    val lc: Byte? = null,
    val data: ByteArray? = null,
    val le: Byte? = null,
) {
    companion object {
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
