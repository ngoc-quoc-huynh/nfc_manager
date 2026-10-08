# nfc_manager_android

The android implementation of the [nfc_manager](../nfc_manager).

# Usage

This package is 
[endorsed](https://docs.flutter.dev/packages-and-plugins/developing-packages#endorsed-federated-plugin),
which means you can simply use nfc_manager normally. This package will be automatically included in
your app when you do.

## Compatibility

Libraries support Flutter 3.35 / Dart 3.9 and later. Android builds require
JDK 17 or later; the plugin targets Java 17 bytecode. Development uses JDK 21.

CI analyzes and tests all libraries on the minimum and development Flutter
SDKs. Android unit tests and example APK builds use the development tooling.
Older Android toolchains are not built in CI.

On AGP 9 hosts, the plugin uses built-in Kotlin when enabled and falls back
to the Kotlin Android plugin when the host opts out.

Interactive examples use Flutter 3.47.6 and AGP 9.4.1 with built-in Kotlin.
They retain `android.newDsl=false` for Flutter's legacy DSL compatibility.
