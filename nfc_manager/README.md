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

Host Card Emulation (HCE) needs an Android service declaration and an AID configuration in your app.
The plugin provides the service, `dev.huynh.nfc_manager_android.hce.HostCardEmulationService`. You
declare it in `AndroidManifest.xml` and create an `apduservice.xml` file in the `xml` directory under
`res`. Android routes commands to the service only when all three requirements hold:

1. The service is declared with `android:exported="true"`.
2. The service requires the permission `android.permission.BIND_NFC_SERVICE`.
3. `apduservice.xml` contains an `aid-filter` for every AID you emulate.

#### Update `AndroidManifest.xml`

Locate your `AndroidManifest.xml` file typically found in `src/main/`. Add the following service
declaration inside the `<application>` tag:

```xml
<service
    android:name="dev.huynh.nfc_manager_android.hce.HostCardEmulationService"
    android:exported="true"
    android:permission="android.permission.BIND_NFC_SERVICE">
    <intent-filter>
        <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
    </intent-filter>

    <meta-data
        android:name="android.nfc.cardemulation.host_apdu_service"
        android:resource="@xml/apduservice" />
</service>
```

#### Create `apduservice.xml`

Navigate to the `res/xml` directory in your project. If the `xml` directory does not exist, create
it. Then, add the following content to a new file named `apduservice.xml`:

```xml
<host-apdu-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/service_description"
    android:requireDeviceUnlock="true">
    <aid-group
        android:category="other"
        android:description="@string/aid_group_description">
        <!-- Use the AID you pass to startEmulation. -->
        <aid-filter android:name="F000000001" />
    </aid-group>
</host-apdu-service>
```

The AID in `aid-filter` must match the AID you pass to `startEmulation`. The
`android:apduServiceBanner` attribute is only needed for an aid group with
`android:category="payment"`, which Android lists in its payment settings. Do not add a banner for
the `other` category.

#### Define Resource Strings

Ensure these strings are defined in your `strings.xml` file located in `res/values/`:

```xml
<resources>
    <string name="service_description">Custom NFC Manager Android Service</string>
    <string name="aid_group_description">Custom AID group for NFC Manager Android</string>
</resources>
```

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
  (tagId) {
    // Use the tag ID, for example to start an APDU exchange.
  },
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
    // Process response.data.
  } else {
    // Handle response.status, or read response.sw1 and response.sw2.
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

### Testing

`NfcManager` has a `const` constructor and no state, so your code can depend on it and a test can
substitute a mock. Add [`mocktail`](https://pub.dev/packages/mocktail) to your `dev_dependencies`.

#### Mock `NfcManager`

Accept an `NfcManager` in the class you test and pass `const NfcManager()` in production:

```dart
import 'package:nfc_manager/nfc_manager.dart';

class NfcStatusChecker {
  const NfcStatusChecker([this._nfcManager = const NfcManager()]);

  final NfcManager _nfcManager;

  Future<bool> get canScan async =>
      await _nfcManager.isNfcSupported() && await _nfcManager.isNfcEnabled();
}
```

Then mock it in the test:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:nfc_manager/nfc_manager.dart';

class MockNfcManager extends Mock implements NfcManager {}

void main() {
  test('canScan is false while NFC is disabled', () async {
    final nfcManager = MockNfcManager();
    when(nfcManager.isNfcSupported).thenAnswer((_) async => true);
    when(nfcManager.isNfcEnabled).thenAnswer((_) async => false);

    expect(await NfcStatusChecker(nfcManager).canScan, isFalse);
  });
}
```

#### Fake the platform

To test code that calls `const NfcManager()` directly, replace the platform implementation instead.
`nfc_manager` does not export `NfcManagerPlatform`, so add `nfc_manager_platform_interface` and
`plugin_platform_interface` to your `dev_dependencies`. Use the same `git` source and `ref` as for
`nfc_manager`, with `path: nfc_manager_platform_interface`:

```yaml
dev_dependencies:
  nfc_manager_platform_interface:
    git:
      url: https://github.com/ngoc-quoc-huynh/nfc_manager
      path: nfc_manager_platform_interface
      ref: main # use the same ref as nfc_manager
  plugin_platform_interface: ^2.1.8
```

Extend `NfcManagerPlatform`, mix in `MockPlatformInterfaceMixin`, and assign the fake to `instance`:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class FakeNfcManagerPlatform extends NfcManagerPlatform
    with MockPlatformInterfaceMixin {
  @override
  Future<bool> isNfcEnabled() async => true;
}

void main() {
  test('reports NFC as enabled', () async {
    NfcManagerPlatform.instance = FakeNfcManagerPlatform();

    expect(await const NfcManager().isNfcEnabled(), isTrue);
  });
}
```

Methods the fake does not override throw `UnimplementedError`.
