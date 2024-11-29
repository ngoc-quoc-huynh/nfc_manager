import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test(
    'default instance is MethodChannelNfcManager.',
    () => expect(
      NfcManagerPlatform.instance,
      isA<MethodChannelNfcManager>(),
    ),
  );

  test('instance can be overridden and token is verified', () {
    final defaultInstance = NfcManagerPlatform.instance;
    addTearDown(() => NfcManagerPlatform.instance = defaultInstance);

    final mockInstance = MockNfcManagerPlatform();
    NfcManagerPlatform.instance = mockInstance;
    expect(NfcManagerPlatform.instance, mockInstance);
  });

  test('foo throws UnimplementedError.', () async {
    await expectLater(
      () => MockNfcManagerPlatform().foo(),
      throwsA(
        isA<UnimplementedError>().having(
          (e) => e.message,
          'message',
          'foo() has not been implemented.',
        ),
      ),
    );
  });
}

final class MockNfcManagerPlatform extends NfcManagerPlatform {}
