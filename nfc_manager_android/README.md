# nfc_manager_android

The android implementation of the [nfc_manager](../nfc_manager).

# Usage

This package is 
[endorsed](https://docs.flutter.dev/packages-and-plugins/developing-packages#endorsed-federated-plugin),
which means you can simply use nfc_manager normally. This package will be automatically included in
your app when you do.

## Compatibility

Libraries support Flutter 3.44 / Dart 3.12 and later. Android builds require
JDK 17 or later; the plugin targets Java 17 bytecode. Development uses JDK 21.

CI runs one reusable workflow (`.github/workflows/_package.yaml`) for each
package, with the tool versions pinned in `mise.toml`. For this package the
"Checks" job runs `make check`, `make test` and `make android` (Android unit
tests and lint). The example debug APK is built by the `nfc_manager` workflow,
not by this package's. The "Minimum Flutter analysis" job only analyzes
`lib` and `test` on the latest patch of the minimum supported Flutter minor.
Older Android toolchains are not built or tested in CI.

On AGP 9 hosts, the plugin uses built-in Kotlin when enabled and falls back
to the Kotlin Android plugin when the host opts out.

The example in this package is a minimal Gradle host for the plugin's Android
checks. It uses the Flutter version pinned in `mise.toml` and AGP 9.4.1 with
built-in Kotlin, and retains `android.newDsl=false` for Flutter's legacy DSL
compatibility. See [`nfc_manager/example`](../nfc_manager/example) for the full
demo.
