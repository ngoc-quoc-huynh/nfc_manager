# nfc_manager_platform_interface

A common platform interface for the [`nfc_manager`](../nfc_manager) plugin.

This interface allows platform-specific implementations of the `nfc_manager`
plugin, as well as the plugin itself, to ensure they are supporting the same
interface. App developers use [`nfc_manager`](../nfc_manager) instead of
depending on this package directly.

## Contents

- [What it provides](#what-it-provides)
- [Implementing a platform](#implementing-a-platform)
- [Testing](#testing)
- [Versioning policy](#versioning-policy)

## What it provides

- [`NfcManagerPlatform`](lib/src/nfc_manager_platform.dart): the abstract class
  that platform implementations extend. Methods that are not overridden throw
  `UnimplementedError`.
- `ApduCommand` and `ApduResponse`: the APDU models exchanged with a tag.
  `ApduStatus` names the status word of a response.
- `HostCardEmulationStatus`: the events reported while emulating a card.
- `NfcException` and its subclasses: the errors that implementations report.
  `NfcUnknownException` carries the platform error `code`.
- `HexString.hexToBytes` and `HexBytes.toHex`: extensions that convert between
  bytes and hexadecimal strings, such as the tag identifiers reported by
  `startDiscovery`.
- `checkAidLength` and `checkPinLength`: the AID and PIN length checks for
  `startEmulation`, which throw `RangeError`.

## Implementing a platform

Extend `NfcManagerPlatform`, override the methods your platform supports, and
set the default instance when your plugin registers. Translate platform errors
into the `NfcException` subclasses so that callers handle one set of errors on
every platform.

```dart
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

class NfcManagerExamplePlatform extends NfcManagerPlatform {
  static const _channel = MethodChannel('example/nfc_manager');
  static const _discovery = EventChannel('example/nfc_manager/discovery');

  /// Registers this implementation as the default [NfcManagerPlatform].
  static void registerWith() {
    NfcManagerPlatform.instance = NfcManagerExamplePlatform();
  }

  @override
  Future<bool> isNfcSupported() async =>
      await _channel.invokeMethod<bool>('isNfcSupported') ?? false;

  @override
  Stream<String> startDiscovery({Duration? timeout}) =>
      _discovery
          .receiveBroadcastStream(timeout?.inMilliseconds)
          .cast<String>()
          .handleError(
            (Object error, StackTrace stackTrace) => Error.throwWithStackTrace(
              _toNfcException(error as PlatformException),
              stackTrace,
            ),
            test: (error) => error is PlatformException,
          );

  @override
  Future<ApduResponse> sendCommand(ApduCommand command) async {
    try {
      final bytes = await _channel.invokeMethod<Uint8List>('sendCommand', {
        'command': command.toUint8List(),
      });
      if (bytes == null || bytes.length < 2) {
        throw const TagConnectionException();
      }
      return ApduResponse.fromUint8List(bytes);
    } on PlatformException catch (e, stackTrace) {
      Error.throwWithStackTrace(_toNfcException(e), stackTrace);
    }
  }

  static NfcException _toNfcException(PlatformException error) =>
      switch (error.code) {
        'ISO_DEP_NOT_SUPPORTED' => const IsoDepNotSupportedException(),
        'TAG_CONNECTION' => const TagConnectionException(),
        _ => NfcUnknownException(code: error.code, message: error.message),
      };
}
```

Document every exception a method reports in its dartdoc, as the methods of
`NfcManagerPlatform` do.

## Testing

Setting `NfcManagerPlatform.instance` verifies a token, so a plain mock is
rejected. Mix `MockPlatformInterfaceMixin` from
[`plugin_platform_interface`](https://pub.dev/packages/plugin_platform_interface)
into a class that implements `NfcManagerPlatform`. Add that package to
`dev_dependencies`.

```dart
import 'package:mockito/mockito.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockNfcManagerPlatform extends Mock
    with MockPlatformInterfaceMixin
    implements NfcManagerPlatform {}
```

`Mock` comes from `package:mockito/mockito.dart` here; `package:mocktail` or a
hand-written fake that implements `NfcManagerPlatform` and uses the mixin works
as well. Assign it with `NfcManagerPlatform.instance = MockNfcManagerPlatform()`
in `setUp`.

## Versioning policy

This package follows [semantic versioning](https://semver.org). Breaking changes
to the interface, such as removing or changing a method signature, an exception
type or a model, are released only in a major version. Adding a method with a
default implementation that throws `UnimplementedError` is not breaking, so
existing implementations keep working.

Platform packages should depend on a caret range of this package, so that they
accept every release within the major version they were written against.
