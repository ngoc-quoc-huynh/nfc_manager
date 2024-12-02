import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_android/nfc_manager_android.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  final platform = NfcManagerAndroidPlatform();
  final logs = <MethodCall>[];

  setUp(logs.clear);

  test('registers instance correctly.', () {
    NfcManagerAndroidPlatform.registerWith();
    expect(NfcManagerPlatform.instance, isA<NfcManagerAndroidPlatform>());
  });

  group('isNfcSupported', () {
    test('returns true if method channel returns true.', () async {
      platform.methodChannel.setMockResponse(logs, true);

      expect(await platform.isNfcSupported(), isTrue);
      expect(
        logs,
        [isMethodCall('isNfcSupported', arguments: null)],
      );
    });

    test('returns true if method channel returns false.', () async {
      platform.methodChannel.setMockResponse(logs, false);

      expect(await platform.isNfcSupported(), isFalse);
      expect(
        logs,
        [isMethodCall('isNfcSupported', arguments: null)],
      );
    });
  });
}

extension<T> on MethodChannel {
  void setMockResponse(List<MethodCall> logs, T response) =>
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        this,
        (methodCall) async {
          logs.add(methodCall);
          return response;
        },
      );
}
