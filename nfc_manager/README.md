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
```

### Android HCE setup

To use the `nfc_manager` plugin on Android, you need to configure your `AndroidManifest.xml` and
create an `apduservice.xml` file in the `xml` directory under `res`.

#### Update `AndroidManifest.xml`

Locate your `AndroidManifest.xml` file typically found in `src/main/`. Add the following service
declaration inside the `<application>` tag:

```xml
<service android:name="dev.huynh.nfc_manager_android.host_card_emulation.HostCardEmulation"
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

### Minimal Examples

#### Checking device capabilities
- **HCE Support**: Determine if Host Card Emulation (HCE) is supported.
  ```dart
  NfcManager().isHceSupported();
  ```
- **NFC Support**: Check if NFC is supported on the device.
  ```dart
  NfcManager().isNfcSupported();
  ```
- **NFC Enabled**: Verify if NFC is enabled.
  ```dart
  NfcManager().isNfcEnabled();
  ```

#### Starting NFC operations
- **Discover NFC Tags**: Begin the discovery of nearby NFC tags.
  ```dart
  NfcManager().startDiscovery();
  ```

#### Communicating with NFC tags
- **Send an APDU Command**: Send a command to an NFC tag using a specific Application Identifier (AID).
  ```dart
  const aid = 'F000000001';
  final command = SelectAidCommand(aid.toUint8List(isHex: true));
  final response = await NfcManager().sendCommand(command);
  ```

#### Emulating an NFC card
- **Start NFC Card Emulation**: Emulate an NFC card using a provided AID and PIN.
  ```dart
  const aid = 'F000000001';
  const pin = '1234';
  final stream = NfcManager().startEmulation(
    aid: aid.toUint8List(isHex: true),
    pin: pin.toUint8List(isHex: true),
  );
  ```
