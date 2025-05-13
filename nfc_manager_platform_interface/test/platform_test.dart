import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  test(
    'default instance is MethodChannelNfcManager.',
    () => expect(NfcManagerPlatform.instance, isA<MethodChannelNfcManager>()),
  );

  test('instance can be overridden and token is verified', () {
    final defaultInstance = NfcManagerPlatform.instance;
    addTearDown(() => NfcManagerPlatform.instance = defaultInstance);

    final mockInstance = MockNfcManagerPlatform();
    NfcManagerPlatform.instance = mockInstance;
    expect(NfcManagerPlatform.instance, mockInstance);
  });

  test('isHceSupported throws UnimplementedError.', () async {
    await expectLater(
      () => MockNfcManagerPlatform().isHceSupported(),
      throwsA(
        isA<UnimplementedError>().having(
          (e) => e.message,
          'message',
          'isHceSupported() has not been implemented.',
        ),
      ),
    );
  });

  test('isNfcSupported throws UnimplementedError.', () async {
    await expectLater(
      () => MockNfcManagerPlatform().isNfcSupported(),
      throwsA(
        isA<UnimplementedError>().having(
          (e) => e.message,
          'message',
          'isNfcSupported() has not been implemented.',
        ),
      ),
    );
  });

  test('isNfcEnabled throws UnimplementedError.', () async {
    await expectLater(
      () => MockNfcManagerPlatform().isNfcEnabled(),
      throwsA(
        isA<UnimplementedError>().having(
          (e) => e.message,
          'message',
          'isNfcEnabled() has not been implemented.',
        ),
      ),
    );
  });

  test('startDiscovery throws UnimplementedError.', () async {
    await expectLater(
      () => MockNfcManagerPlatform().startDiscovery(),
      throwsA(
        isA<UnimplementedError>().having(
          (e) => e.message,
          'message',
          'startDiscovery() has not been implemented.',
        ),
      ),
    );
  });

  test('sendCommand throws UnimplementedError.', () async {
    await expectLater(
      () =>
          MockNfcManagerPlatform().sendCommand(SelectAidCommand(Uint8List(0))),
      throwsA(
        isA<UnimplementedError>().having(
          (e) => e.message,
          'message',
          'sendCommand() has not been implemented.',
        ),
      ),
    );
  });

  test('startEmulation throws UnimplementedError.', () async {
    await expectLater(
      () => MockNfcManagerPlatform().startEmulation(
        aid: Uint8List(0),
        pin: Uint8List(0),
      ),
      throwsA(
        isA<UnimplementedError>().having(
          (e) => e.message,
          'message',
          'startEmulation() has not been implemented.',
        ),
      ),
    );
  });

  test('isPlatformException returns correctly.', () {
    expect(
      MockNfcManagerPlatform().isPlatformException(
        PlatformException(code: 'code'),
      ),
      isTrue,
    );
    expect(MockNfcManagerPlatform().isPlatformException(''), isFalse);
  });

  test('onStreamError throws correctly.', () {
    expect(
      () => MockNfcManagerPlatform().onStreamError(
        PlatformException(code: 'code'),
      ),
      throwsA(isA<NfcException>()),
    );
    expect(
      () => MockNfcManagerPlatform().onStreamError(''),
      throwsA(
        isA<NfcUnknownException>().having((e) => e.message, 'message', ''),
      ),
    );
  });
}

final class MockNfcManagerPlatform extends NfcManagerPlatform {
  MockNfcManagerPlatform() : super('test');
}
