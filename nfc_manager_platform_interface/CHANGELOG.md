## 2.0.0

### Breaking changes

- Require Flutter 3.44.0 and Dart 3.12.0 or newer (was Flutter 3.41 and Dart 3.0). Upgrade the SDK; no API change.
- `NfcManagerPlatform` no longer carries the method channel. `MethodChannelNfcManager`, `MockNfcManagerPlatformMixin`, the `platform` constructor argument and the channel fields are removed. Platform implementations extend `NfcManagerPlatform()` and bring their own transport.
- Rename `Command` to `ApduCommand` (`apdu_command.dart`). Constructor and factories are unchanged. Change `sendCommand(Command)` to `sendCommand(ApduCommand)` in platform implementations.
- `ApduCommand` is final and non-const. Replace `SelectAidCommand` and `VerifyPinCommand` with `ApduCommand.selectAid` and `ApduCommand.verifyPin`. Lc is derived from the payload and the data is copied.
- The name `ApduResponse` changes meaning. In 1.2.0 it was the status enum that `sendCommand` returned. It is now a class (`data`, `sw1`, `sw2`, `statusWord`, `isSuccess`, `status`), and the enum is renamed `ApduStatus`. Switch on `response.status` and write `ApduStatus.x` where you wrote `ApduResponse.x`. Payload responses keep their `data` and classify the trailing status word (they were `unknown`).
- Rename the status values to ISO 7816-4 names: `ok` to `success` (`90 00`), `wrongPin` to `verificationFailed` (`63 00`, and now also `63 CX` with X retries left), `hceNotReady` to `securityStatusNotSatisfied` (`69 82`), `nullCommand` to `wrongParameters` (`6A 00`), `invalidAid` to `fileOrApplicationNotFound` (`6A 82`). `functionNotSupported`, `wrongLength` and `unknown` keep their names. `wrongLength` still covers `67 00`, but `6C XX` is now the new `wrongLeField` (1.2.0 reported `6C 00` as `wrongLength`); add a case for it if you handled `6C 00`. `wrongLcLength` is removed; 1.2.0 never reported it.
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

## 1.2.0

- Set Dart constraint to `^3.0.0`.
- Set Flutter constraint to `^3.41.0`.

## 1.1.0

- Update dependencies.
- Update Dart to `3.8.0`.

## 1.0.0

- Initial release.
