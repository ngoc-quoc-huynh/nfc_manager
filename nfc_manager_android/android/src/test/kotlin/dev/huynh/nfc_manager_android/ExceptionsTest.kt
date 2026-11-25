package dev.huynh.nfc_manager_android

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExceptionsTest {
    @Test
    fun `each NfcException reports its Flutter error code`() =
        listOf(
            InvalidArgumentsException(),
            InvalidHceArgumentsException(),
            InvalidApduCommandException(),
            PinBlockedException(),
            IsoDepNotSupportedException(),
            NfcNotSupportedException(),
            TagConnectionException(),
            UnexpectedNfcException(),
        ).forEach { exception -> assertEquals(expectedCode(exception), exception.code) }

    /**
     * The codes are the contract with the Dart side; change them only together with it.
     *
     * The `when` is exhaustive, so a new [NfcException] does not compile until it has a code here.
     */
    private fun expectedCode(exception: NfcException) =
        when (exception) {
            is InvalidArgumentsException, is InvalidHceArgumentsException -> "INVALID_ARGUMENTS"
            is InvalidApduCommandException -> "INVALID_APDU_COMMAND"
            is PinBlockedException -> "PIN_BLOCKED"
            is IsoDepNotSupportedException -> "ISO_DEP_NOT_SUPPORTED"
            is NfcNotSupportedException -> "NFC_NOT_SUPPORTED"
            is TagConnectionException -> "TAG_CONNECTION_FAILED"
            is UnexpectedNfcException -> "NFC_ERROR"
        }
}
