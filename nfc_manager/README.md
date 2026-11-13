# nfc_manager

[![build status](https://github.com/ngoc-quoc-huynh/nfc_manager/actions/workflows/nfc_manager.yaml/badge.svg?branch=main)](https://github.com/ngoc-quoc-huynh/nfc_manager/actions/workflows/nfc_manager.yaml?query=branch%3Amain)
[![style](https://img.shields.io/badge/style-cosee__lints-brightgreen)](https://pub.dev/packages/cosee_lints)
[![release](https://img.shields.io/github/v/release/ngoc-quoc-huynh/nfc_manager)](https://github.com/ngoc-quoc-huynh/nfc_manager/releases/latest)
[![license](https://img.shields.io/github/license/ngoc-quoc-huynh/nfc_manager)](https://raw.githubusercontent.com/ngoc-quoc-huynh/nfc_manager/refs/heads/main/LICENSE)

The `nfc_manager` is a comprehensive Flutter plugin designed to facilitate easy and efficient
interaction with a device's NFC capabilities. It provides a streamlined, high-level API for NFC
operations, ensuring seamless integration into your Flutter applications.

## Features

- **NFC Discovery**: Enables the detection of NFC tags and devices in proximity.
- **Host Card Emulation (HCE)**: Supports emulating NFC cards, allowing devices to act like an NFC
  card.
- **APDU Commands**: Facilitates sending and receiving APDU commands, essential for advanced NFC
  operations.

## Usage

To use the `nfc_manager` plugin, add it to your `pubspec.yaml` file:

```yaml
dependencies:
  nfc_manager:
    git:
      url: https://github.com/ngoc-quoc-huynh/nfc_manager
      path: nfc_manager
      ref: main # replace with a release tag or commit SHA to pin a version
```

### Android HCE setup

To use the `nfc_manager` plugin on Android, you need to configure your `AndroidManifest.xml` and
create an `apduservice.xml` file in the `xml` directory under `res`.

#### Update `AndroidManifest.xml`

Locate your `AndroidManifest.xml` file typically found in `src/main/`. Add the following service
declaration inside the `<application>` tag:

```xml
<service android:name="dev.huynh.nfc_manager_android.hce.HostCardEmulationService"
    android:exported="true" android:permission="android.permission.BIND_NFC_SERVICE">
    <intent-filter>
        <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
    </intent-filter>
    <meta-data android:name="android.nfc.cardemulation.host_apdu_service"
        android:resource="@xml/apduservice" />
</service>
```

#### Create `apduservice.xml`

Navigate to the `res/xml` directory in your project. If the `xml` directory does not exist, create
it. Then, add the following content to a new file named `apduservice.xml`:

```xml
<host-apdu-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:apduServiceBanner="@drawable/launch_background"
    android:description="@string/service_description" android:requireDeviceUnlock="true">
    <aid-group android:category="other" android:description="@string/aid_group_description">
        <!-- This AID can be customized as needed -->
        <aid-filter android:name="F000000001" />
    </aid-group>
</host-apdu-service>
```

#### Define Resource Strings

Ensure these strings are defined in your `strings.xml` file located in `res/values/`:

```xml
<resources>
    <string name="service_description">Custom NFC Manager Android Service</string>
    <string name="aid_group_description">Custom AID group for NFC Manager Android</string>
</resources>
```

This setup ensures that your Android application is prepared to handle NFC card emulation via the
`nfc_manager` plugin. 

### Platform support

Android only. Tag discovery and APDU exchange work with ISO-DEP (ISO 14443-4) tags. Other platforms
throw `UnimplementedError`.

### Minimal Examples

Import the package in each snippet below:

```dart
import 'dart:convert';

import 'package:nfc_manager/nfc_manager.dart';
```

#### Checking device capabilities

```dart
const manager = NfcManager();

final hceSupported = await manager.isHceSupported();
final nfcSupported = await manager.isNfcSupported();
final nfcEnabled = await manager.isNfcEnabled();
```

#### Discovering NFC tags

`startDiscovery` returns a stream. Discovery starts when you listen and stops when you cancel the
subscription. Each event is the tag ID as uppercase hexadecimal, for example `04A1B2C3`. The optional
`timeout` limits communication with a discovered tag, not the wait for a tag.

```dart
final subscription = const NfcManager().startDiscovery().listen(
  (tagId) => print('Found tag $tagId'),
  onError: (Object error) {
    if (error is IsoDepNotSupportedException) {
      // The tag does not support ISO-DEP.
    }
  },
);

// Later, to stop discovery:
await subscription.cancel();
```

#### Communicating with NFC tags

Send an APDU command to the discovered tag. `ApduResponse` keeps the response `data` and the status
bytes `sw1` and `sw2`.

```dart
const aid = 'F000000001';

try {
  final response = await const NfcManager().sendCommand(
    ApduCommand.selectAid(aid.hexToBytes()),
  );
  if (response.isSuccess) {
    print('Selected, data: ${response.data.toHex()}');
  } else {
    print('Status ${response.status.name}: ${response.statusWord.toRadixString(16)}');
  }
} on TagConnectionException {
  // There is no connection, the tag was lost, or it returned fewer than 2 bytes.
} on NfcException catch (e) {
  // Another plugin error. Treat `e.message` as diagnostic text only.
}
```

#### Emulating an NFC card

Emulation starts when you listen to the stream and stops when you cancel the subscription. Encode the
PIN yourself, here as UTF-8 bytes (`31 32 33 34`).

```dart
const aid = 'F000000001';
const pin = '1234';

final subscription = const NfcManager()
    .startEmulation(aid: aid.hexToBytes(), pin: utf8.encode(pin))
    .listen((status) {
      if (status == HostCardEmulationStatus.pinVerified) {
        // The reader sent the correct PIN.
      }
    });

// Later, to stop emulation:
await subscription.cancel();
```

#### Handling errors

- `startDiscovery` throws `ArgumentError` when `timeout` is not a positive whole number of
  milliseconds up to 2147483647.
- `startEmulation` throws `RangeError` when the AID has fewer than 5 or more than 16 bytes, or the
  PIN has fewer than 1 or more than 255 bytes.
- `hexToBytes` throws `FormatException` for an odd length or a non-hexadecimal character.
- All other errors are subtypes of `NfcException`. `NfcUnknownException` carries the platform's
  `code`. The `message` of any `NfcException` is untrusted diagnostic text: do not parse it or rely
  on it.
