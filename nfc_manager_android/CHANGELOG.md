## 2.0.0

### Breaking changes

- Require Flutter 3.44.0 and Dart 3.12.0 or newer (was Flutter 3.41 and Dart 3.0). Upgrade the SDK; no API change.
- Require minSdk 24 (was 21). Apps with a lower minSdk must raise it. Apps using `flutter.minSdkVersion` need no change.
- HCE answers commands shorter than 4 bytes with `67 00` (wrong length) instead of `6C 00`. Readers that matched `6C 00` must match `67 00`. Readers on this plugin still get `ApduStatus.wrongLength`, which now covers only `67 00`; `6C 00` is `wrongLeField`. HCE event names sent to Dart are unchanged.
- Rename the HCE service in the manifest. Change `<service android:name>` from `dev.huynh.nfc_manager_android.host_card_emulation.HostCardEmulation` to `dev.huynh.nfc_manager_android.hce.HostCardEmulationService`. Permission, intent filter and `apduservice.xml` meta-data are unchanged. The old name fails when Android binds the service.
- `package:nfc_manager_android/nfc_manager_android.dart` exports only `NfcManagerAndroidPlatform`. Import shared types (`NfcManagerPlatform`, `ApduCommand`, `ApduResponse`, the `NfcException` family) from `nfc_manager_platform_interface` and add it as a dependency.
- `sendCommand` returns the new `ApduResponse` class. The 1.2.0 status enum, also named `ApduResponse`, is now `ApduStatus` and is read as `response.status` (see `nfc_manager_platform_interface` 2.0.0 for the value mapping). A tag response shorter than 2 bytes throws `TagConnectionException`.
- Wire decoding is private to this package. An unknown HCE event name is now `NfcUnknownException` with code `UNKNOWN_HCE_STATUS` (was `ArgumentError`).
- `InvalidLcDataLengthException` is gone. A malformed `sendCommand` argument arrives as `NfcUnknownException` with code `INVALID_ARGUMENTS`.

### Behaviour changes

- A 6-byte short APDU with Lc = 00 (`CLA INS P1 P2 00 XX`) is rejected. HCE answers `67 00` and reports `INVALID_LC_LENGTH` (an unsupported INS used to get `6A 81`).
- Native discovery failures (bad arguments, `enableReaderMode` errors) arrive on the discovery stream as errors with codes `INVALID_ARGUMENTS` or `NFC_ERROR`, instead of only as `FlutterError` reports.
- The discovery stream ends when the activity detaches. To resume, call `startDiscovery()` again after the activity reattaches.
- Malformed HCE arguments (`aid`, `pin`) are reported as `INVALID_ARGUMENTS`, end the HCE stream, and leave HCE unconfigured.
- A `sendCommand` APDU whose Lc does not match its data length is reported as `INVALID_APDU_COMMAND`.
- HCE ends the session after 3 wrong PINs in a row, so readers cannot brute-force the PIN. The third wrong VERIFY is answered with `69 83`, and the HCE stream reports `NfcUnknownException` with code `PIN_BLOCKED` and then ends. The count survives reader disconnects and reselection; a correct PIN resets it. Start emulation again to accept PINs.

### Fixes

- Compare HCE PINs in constant time.
- Stop HCE responses after the Flutter engine detaches; they used to keep answering SELECT and VERIFY with `90 00`.
- Reject HCE SELECT and VERIFY commands without data instead of crashing.
- Serialize tag I/O on one worker and release reader sessions and connections on detach.
- Report commands longer than the tag accepts as `INVALID_APDU_COMMAND` without dropping the tag.
- Tolerate `disableReaderMode` failures, which could crash the app when the activity was destroyed.
- Close tags outside the state lock so the main thread does not block on tag I/O.
- Native failures use defined error codes and no longer include raw exception text.

## 1.2.0

- Set Dart constraint to `^3.0.0`.
- Set Flutter constraint to `^3.41.0`.

## 1.1.0

- Update dependencies.
- Update Dart to `3.8.0`.

## 1.0.1

- Handle SecurityException on isoDep.close ().

## 1.0.0

- Initial release.
