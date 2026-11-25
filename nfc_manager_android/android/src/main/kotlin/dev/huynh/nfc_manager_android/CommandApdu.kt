package dev.huynh.nfc_manager_android

/** A parsed short Application Protocol Data Unit (APDU) command (ISO/IEC 7816-4). */
internal class CommandApdu private constructor(
    /** The command class byte (CLA). */
    val cla: Byte,
    /** The instruction byte (INS). */
    val ins: Byte,
    /** The first instruction parameter byte (P1). */
    val p1: Byte,
    /** The second instruction parameter byte (P2). */
    val p2: Byte,
    /** The payload, or null for a command without data. */
    val data: ByteArray?,
    /**
     * The encoded expected response data length (Le), or null when omitted.
     *
     * In a short APDU, zero encodes an expected length of 256 bytes.
     */
    val le: Byte?,
) {
    companion object {
        private const val HEADER_LENGTH = 4
        private const val DATA_OFFSET = HEADER_LENGTH + 1

        /**
         * Parses a short APDU. Lc and Le each take one byte; extended-length encodings are rejected.
         *
         * @throws InvalidApduCommandException if the command is shorter than 4 bytes.
         * @throws InvalidLcDataLengthException if Lc is `00` or does not match the data length.
         */
        fun fromByteArray(command: ByteArray): CommandApdu {
            if (command.size < HEADER_LENGTH) throw InvalidApduCommandException()
            val (cla, ins, p1, p2) = command
            if (command.size <= DATA_OFFSET) {
                return CommandApdu(cla, ins, p1, p2, data = null, le = command.getOrNull(HEADER_LENGTH))
            }

            val lc = command[HEADER_LENGTH].toUnsignedInt()
            val dataEnd = DATA_OFFSET + lc
            // Lc 00 starts an extended-length encoding, which short APDUs do not allow.
            if (lc == 0 || command.size !in dataEnd..dataEnd + 1) throw InvalidLcDataLengthException()
            return CommandApdu(
                cla,
                ins,
                p1,
                p2,
                data = command.sliceArray(DATA_OFFSET until dataEnd),
                le = command.getOrNull(dataEnd),
            )
        }
    }
}
