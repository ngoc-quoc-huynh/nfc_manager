## 2.0.0

### Breaking changes

- Require Flutter 3.44.0 and Dart 3.12.0 or newer (was 3.35 and 3.9). Upgrade the SDK; no API change.
- `NfcManagerPlatform` no longer carries the method channel. `MethodChannelNfcManager`, `MockNfcManagerPlatformMixin`, the `platform` constructor argument and the channel fields are removed. Platform implementations extend `NfcManagerPlatform()` and bring their own transport.
- Rename `Command` to `ApduCommand` (`apdu_command.dart`). Constructor and factories are unchanged. Change `sendCommand(Command)` to `sendCommand(ApduCommand)` in platform implementations.
- `ApduCommand` is final and non-const. Replace `SelectAidCommand` and `VerifyPinCommand` with `ApduCommand.selectAid` and `ApduCommand.verifyPin`. Lc is derived from the payload and the data is copied.
- `sendCommand` returns an `ApduResponse` (`data`, `sw1`, `sw2`, `statusWord`, `isSuccess`, `status`) instead of an enum. Read the enum as `response.status`; it is now `ApduStatus`. Payload responses keep their `data` and classify the trailing status word (they were `unknown`).
- Rename the `ApduStatus` values to ISO 7816-4 names: `ok` to `success`, `wrongPin` to `verificationFailed`, `wrongLcLength` to `wrongLength` (`67 00`), `wrongLength` to `wrongLeField` (`6C 00`), `hceNotReady` to `securityStatusNotSatisfied`, `nullCommand` to `wrongParameters`, `invalidAid` to `fileOrApplicationNotFound`. `wrongLength` now has a different meaning, so check every `switch` on it.
- Replace `StringExtensions` with `HexString.hexToBytes()` and `HexBytes.toHex()` (uppercase output). Invalid hex throws `FormatException` with the offset only. Text and PINs are encoded with `utf8.encode`, so a PIN such as "1234" is now 4 bytes. Readers and HCE configured from the old examples must match.
- Remove `NfcException.fromPlatformException` and `HostCardEmulationStatus.fromString`. Custom platforms map their own errors and event names.
- `NfcUnknownException` requires `code` and takes `message`. `code` is part of `==`, `hashCode` and `toString()`. Update constructor calls.
- Remove `InvalidLcDataLengthException`; it could not reach Dart. `InvalidApduCommandException` now means the platform rejected the command (for example, longer than the tag accepts), and its message changed.
- Stop re-exporting `MockPlatformInterfaceMixin`. Tests that mock `NfcManagerPlatform` add `plugin_platform_interface` as a dev dependency and import it directly.

### Behaviour changes

- `ApduCommand.verifyPin` reports a bad PIN length as `RangeError` on `pin.length` (was `data.length`). The accepted range is still 1 to 255.

### Fixes

- Copy the APDU response payload so changes to the source buffer cannot alter the response.
- Reject malformed hex input and encode text as UTF-8, including multibyte characters.
- Include the exception type in `hashCode`, so different exception types with the same message no longer collide.

### Other

- Add `checkAidLength` and `checkPinLength` for validating emulation input (5 to 16 bytes for the AID, 1 to 255 for the PIN).
- Remove the example app; `nfc_manager/example` is the full demo.

## 1.2.0

- Set Dart constraint to `^3.0.0`.
- Set Flutter constraint to `^3.41.0`.

## 1.1.0

- Update dependencies.
- Update Dart to `3.8.0`.

## 1.0.0

- Initial release.
