import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  final defaultPlatform = NfcManagerPlatform.instance;

  tearDown(() => NfcManagerPlatform.instance = defaultPlatform);

  test('throws UnimplementedError when no platform is registered.', () {
    expect(defaultPlatform.isHceSupported, throwsUnimplementedError);
    expect(defaultPlatform.isNfcSupported, throwsUnimplementedError);
    expect(defaultPlatform.isNfcEnabled, throwsUnimplementedError);
    expect(defaultPlatform.startDiscovery, throwsUnimplementedError);
    expect(
      () => defaultPlatform.sendCommand(
        Command(cla: 0, ins: 0, p1: 0, p2: 0),
      ),
      throwsUnimplementedError,
    );
    expect(
      () =>
          defaultPlatform.startEmulation(aid: Uint8List(5), pin: Uint8List(1)),
      throwsUnimplementedError,
    );
  });

  test('a subclass can register and supply platform behavior.', () async {
    NfcManagerPlatform.instance = TestNfcManagerPlatform();

    expect(await NfcManagerPlatform.instance.isNfcSupported(), isTrue);
    expect(
      NfcManagerPlatform.instance.isHceSupported,
      throwsUnimplementedError,
    );
  });

  test('rejects implementations without the platform token.', () {
    expect(
      () => NfcManagerPlatform.instance = InvalidNfcManagerPlatform(),
      throwsA(isA<AssertionError>()),
    );
    expect(NfcManagerPlatform.instance, same(defaultPlatform));
  });

  test('accepts the standard mock platform mixin in tests.', () {
    final mock = MockNfcManagerPlatform();
    NfcManagerPlatform.instance = mock;

    expect(NfcManagerPlatform.instance, same(mock));
  });
}

final class TestNfcManagerPlatform extends NfcManagerPlatform {
  @override
  Future<bool> isNfcSupported() async => true;
}

final class InvalidNfcManagerPlatform extends Fake
    implements NfcManagerPlatform {}

final class MockNfcManagerPlatform extends Fake
    with MockPlatformInterfaceMixin
    implements NfcManagerPlatform {}
