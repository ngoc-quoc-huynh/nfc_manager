# nfc_manager

[![build status](https://github.com/ngoc-quoc-huynh/nfc_manager/actions/workflows/nfc_manager.yaml/badge.svg?branch=main)](https://github.com/ngoc-quoc-huynh/nfc_manager/actions/workflows/nfc_manager.yaml?query=branch%3Amain)
[![style](https://img.shields.io/badge/style-cosee__lints-brightgreen)](https://pub.dev/packages/cosee_lints)
[![release](https://img.shields.io/github/v/release/ngoc-quoc-huynh/nfc_manager)](https://github.com/ngoc-quoc-huynh/nfc_manager/releases/latest)
[![license](https://img.shields.io/github/license/ngoc-quoc-huynh/nfc_manager)](https://raw.githubusercontent.com/ngoc-quoc-huynh/nfc_manager/refs/heads/main/LICENSE)

Discover ISO-DEP tags, exchange APDU commands, and emulate cards with Host Card
Emulation (HCE) on Android.

## Install

This package is not on pub.dev. Add it as a git dependency and pin a release
tag:

```yaml
dependencies:
  nfc_manager:
    git:
      url: https://github.com/ngoc-quoc-huynh/nfc_manager
      path: nfc_manager
      ref: v2.0.0
```

It requires Flutter 3.44 and Dart 3.12 or later. The Android implementation and
the NFC permission are included automatically.

## Usage

```dart
import 'dart:convert';

import 'package:nfc_manager/nfc_manager.dart';

const nfcManager = NfcManager();
```

## Read a tag

Check `isNfcEnabled()`, then listen to `startDiscovery()`. Each event is the ID
of a connected tag, and `sendCommand()` talks to that tag:

```dart
final subscription = nfcManager.startDiscovery().listen((tagId) async {
  final response = await nfcManager.sendCommand(
    ApduCommand.selectAid('F000000001'.hexToBytes()),
  );
  print(response.status);
});
```

Cancel the subscription to stop discovery.

## Emulate a card

1. Declare the HCE service inside `<application>` in
   `android/app/src/main/AndroidManifest.xml`:

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

2. Create `android/app/src/main/res/xml/apduservice.xml` with your AID:

   ```xml
   <host-apdu-service xmlns:android="http://schemas.android.com/apk/res/android"
       android:description="@string/service_description"
       android:requireDeviceUnlock="true">
       <aid-group
           android:category="other"
           android:description="@string/aid_group_description">
           <aid-filter android:name="F000000001" />
       </aid-group>
   </host-apdu-service>
   ```

3. Add both strings to `android/app/src/main/res/values/strings.xml`:

   ```xml
   <resources>
       <string name="service_description">My NFC card</string>
       <string name="aid_group_description">My card application</string>
   </resources>
   ```

4. Check `isHceSupported()` and `isNfcEnabled()`, then start emulation with
   the same AID:

   ```dart
   final subscription = nfcManager
       .startEmulation(aid: 'F000000001'.hexToBytes(), pin: utf8.encode('1234'))
       .listen(print);
   ```

Cancel the subscription to stop emulation.

## Example

The [example app](example/) reads tags and emulates the demo card. See its
[README](example/README.md) for what to try.
