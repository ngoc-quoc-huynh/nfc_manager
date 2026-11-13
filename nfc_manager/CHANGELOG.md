## 2.0.0

### Breaking changes

- Require Flutter 3.44.0 and Dart 3.12.0 or newer (was 3.35 and 3.9). Upgrade the SDK; no API change.
- Require Android minSdk 24 (was 21) through `nfc_manager_android`. Apps using `flutter.minSdkVersion` need no change.
- `startEmulation` throws `RangeError` synchronously, before returning a stream, unless `aid` is 5 to 16 bytes (`aid.length`) and `pin` is 1 to 255 bytes (`pin.length`). Validate user input or catch the error at the call site.
- `startDiscovery(timeout:)` throws `ArgumentError` (name `timeout`) synchronously unless the timeout is a positive whole number of milliseconds up to 2147483647. In 1.x invalid values were reported to `FlutterError` and the stream stayed silent; fractional milliseconds were truncated. `null` is still allowed.
- `package:nfc_manager` exports only `NfcManager`, `ApduCommand`, `ApduResponse`, `ApduStatus`, `HostCardEmulationStatus`, `NfcException` and its subtypes, and `HexString`/`HexBytes`. Code that uses `NfcManagerPlatform` (for example fake platforms in tests) imports `nfc_manager_platform_interface` directly. `MockPlatformInterfaceMixin` now comes from `plugin_platform_interface`.
- Rename `Command` to `ApduCommand`. Replace `SelectAidCommand` and `VerifyPinCommand` with `ApduCommand.selectAid` and `ApduCommand.verifyPin`.
- `sendCommand` returns an `ApduResponse` instead of an enum; use `response.status` (`ApduStatus`). Values were renamed to ISO 7816-4 names: `ok` to `success`, `wrongPin` to `verificationFailed`, `wrongLcLength` to `wrongLength` (`67 00`), `wrongLength` to `wrongLeField` (`6C 00`), `hceNotReady` to `securityStatusNotSatisfied`, `nullCommand` to `wrongParameters`, `invalidAid` to `fileOrApplicationNotFound`.
- Replace `StringExtensions` with `HexString.hexToBytes()` and `HexBytes.toHex()`. PINs are encoded as UTF-8, so the PIN "1234" is now 4 bytes; readers and HCE configured from the old examples must match.
- `NfcUnknownException` has a required `code` that is part of `==` and `toString()`. On Android, branch on `INVALID_ARGUMENTS`, `NFC_ERROR` or `UNKNOWN_HCE_STATUS`.
- `InvalidLcDataLengthException` is removed. A command the platform rejects throws `InvalidApduCommandException`.
- Android HCE apps change the manifest `<service android:name>` to `dev.huynh.nfc_manager_android.hce.HostCardEmulationService` (see `nfc_manager_android` 2.0.0).

### Behaviour changes

- `NfcManager` is a plain `const NfcManager()` and no longer a singleton. Two instances are not `identical()`.
- Android HCE answers commands shorter than 4 bytes and short APDUs with Lc = 00 with `67 00`. Readers that matched `6C 00` must match `67 00`.
- Discovery failures arrive as stream errors instead of only as `FlutterError` reports. On Android the discovery stream ends when the activity detaches.

### Fixes

- Android: constant-time HCE PIN comparison, no HCE responses after engine detach, serialized tag I/O, and no tag drop for oversized commands (see `nfc_manager_android` 2.0.0).
- Hex parsing rejects malformed input.

### Other

- Update the README and API documentation for the 2.0 API and add Android HCE setup and testing sections.
- Example app shows errors from status futures.

## 1.2.0

- Set Dart constraint to `^3.0.0`.
- Set Flutter constraint to `^3.41.0`.

## 1.1.0

- Update dependencies.
- Update Dart to `3.8.0`.

## 1.0.0

- Initial release.
