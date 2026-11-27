## 2.0.0

- Require Dart `^3.12.0` and Flutter `>=3.44.0`.
- Replace `Command` with immutable `ApduCommand`. Use `ApduCommand.selectAid` and
  `ApduCommand.verifyPin` in place of `SelectAidCommand` and `VerifyPinCommand`.
- `sendCommand` returns an `ApduResponse` with response bytes and an `ApduStatus`.
- Replace `StringExtensions` with `HexString` and `HexBytes`.
- `NfcUnknownException` includes the native error code. Remove `InvalidLcDataLengthException`.
- Remove `MethodChannelNfcManager`. Register a platform implementation before making NFC calls.
- Import `MockPlatformInterfaceMixin` from `plugin_platform_interface`.

## 1.2.0

- Set Dart constraint to `^3.0.0`.
- Set Flutter constraint to `^3.41.0`.

## 1.1.0

- Update dependencies.
- Update Dart to `3.8.0`.

## 1.0.0

- Initial release.
