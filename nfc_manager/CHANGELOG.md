## 2.0.0

- Require Dart `^3.12.0` and Flutter `>=3.44.0`.
- Rename `Command` to `ApduCommand`. Use `ApduCommand.selectAid` and `ApduCommand.verifyPin`
  in place of `SelectAidCommand` and `VerifyPinCommand`.
- `sendCommand` returns an `ApduResponse` with response bytes and an `ApduStatus`.
- Remove `String.toHexString()` and `String.toUint8List()`. Use `hexToBytes()` to decode hex,
  `toHex()` to encode bytes as hex, and `utf8.encode()` to encode text.
- `NfcUnknownException` includes the native error code. Remove `InvalidLcDataLengthException`.
- Replace the `NfcManager` singleton with a public `const` constructor.
- Import platform types directly from `nfc_manager_platform_interface`.

## 1.2.0

- Set Dart constraint to `^3.0.0`.
- Set Flutter constraint to `^3.41.0`.

## 1.1.0

- Update dependencies.
- Update Dart to `3.8.0`.

## 1.0.0

- Initial release.
