# nfc_manager

[![nfc_manager](https://github.com/ngoc-quoc-huynh/nfc_manager/actions/workflows/nfc_manager.yaml/badge.svg?branch=main)](https://github.com/ngoc-quoc-huynh/nfc_manager/actions/workflows/nfc_manager.yaml?query=branch%3Amain)
[![nfc_manager_android](https://github.com/ngoc-quoc-huynh/nfc_manager/actions/workflows/nfc_manager_android.yaml/badge.svg?branch=main)](https://github.com/ngoc-quoc-huynh/nfc_manager/actions/workflows/nfc_manager_android.yaml?query=branch%3Amain)
[![nfc_manager_platform_interface](https://github.com/ngoc-quoc-huynh/nfc_manager/actions/workflows/nfc_manager_platform_interface.yaml/badge.svg?branch=main)](https://github.com/ngoc-quoc-huynh/nfc_manager/actions/workflows/nfc_manager_platform_interface.yaml?query=branch%3Amain)
[![renovate enabled](https://img.shields.io/badge/renovate-enabled-brightgreen.svg)](https://renovatebot.com/)
[![release](https://img.shields.io/github/v/release/ngoc-quoc-huynh/nfc_manager)](https://github.com/ngoc-quoc-huynh/nfc_manager/releases/latest)
[![license](https://img.shields.io/github/license/ngoc-quoc-huynh/nfc_manager)](https://raw.githubusercontent.com/ngoc-quoc-huynh/nfc_manager/refs/heads/main/LICENSE)

A Flutter plugin for Android that reads ISO-DEP NFC tags with APDU commands and
lets a phone act as a contactless card through Host Card Emulation (HCE).

## Try the example

You need Flutter 3.47.6 or later, JDK 21, and an Android phone with NFC turned
on. Card emulation needs a second phone as the reader.

```sh
cd nfc_manager/example
flutter run
```

The [example README](nfc_manager/example/README.md) walks you through reading a
tag and emulating a card.

## Use it in your app

The package supports Flutter 3.44 and later, and is not on pub.dev. The
[nfc_manager README](nfc_manager/README.md) shows the git dependency, the
Android HCE setup, and code to read tags and emulate a card.

## Packages

| Package | Role |
| --- | --- |
| [`nfc_manager`](nfc_manager/) | Public Dart API for Flutter apps. |
| [`nfc_manager_android`](nfc_manager_android/) | Android NFC integration and HCE service. |
| [`nfc_manager_platform_interface`](nfc_manager_platform_interface/) | Shared contract for platform implementations. |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.
