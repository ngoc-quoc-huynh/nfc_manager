import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  final nfcManager = MethodChannelNfcManager();
  final logs = <MethodCall>[];

  setUp(logs.clear);

  group('isHceSupported', () {
    test('returns true if method channel returns true.', () async {
      nfcManager.methodChannel.setMockResponse(logs, true);

      expect(await nfcManager.isHceSupported(), isTrue);
      expect(
        logs,
        [isMethodCall('isHceSupported', arguments: null)],
      );
    });

    test('returns true if method channel returns false.', () async {
      nfcManager.methodChannel.setMockResponse(logs, false);

      expect(await nfcManager.isHceSupported(), isFalse);
      expect(
        logs,
        [isMethodCall('isHceSupported', arguments: null)],
      );
    });
  });

  group('isNfcSupported', () {
    test('returns true if method channel returns true.', () async {
      nfcManager.methodChannel.setMockResponse(logs, true);

      expect(await nfcManager.isNfcSupported(), isTrue);
      expect(
        logs,
        [isMethodCall('isNfcSupported', arguments: null)],
      );
    });

    test('returns true if method channel returns false.', () async {
      nfcManager.methodChannel.setMockResponse(logs, false);

      expect(await nfcManager.isNfcSupported(), isFalse);
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
