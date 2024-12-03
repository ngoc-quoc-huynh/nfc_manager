import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

import 'utils.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  final nfcManager = MethodChannelNfcManager();
  const methodChannel = MethodChannelNfcManager.methodChannel;
  final logs = <MethodCall>[];

  tearDown(logs.clear);

  group('isHceSupported', () {
    test('returns true if method channel returns true.', () async {
      methodChannel.setMockResponse(logs, true);

      expect(await nfcManager.isHceSupported(), isTrue);
      expect(
        logs,
        [isMethodCall('isHceSupported', arguments: null)],
      );
    });

    test('returns true if method channel returns false.', () async {
      methodChannel.setMockResponse(logs, false);

      expect(await nfcManager.isHceSupported(), isFalse);
      expect(
        logs,
        [isMethodCall('isHceSupported', arguments: null)],
      );
    });
  });

  group('isNfcSupported', () {
    test('returns true if method channel returns true.', () async {
      methodChannel.setMockResponse(logs, true);

      expect(await nfcManager.isNfcSupported(), isTrue);
      expect(
        logs,
        [isMethodCall('isNfcSupported', arguments: null)],
      );
    });

    test('returns true if method channel returns false.', () async {
      methodChannel.setMockResponse(logs, false);

      expect(await nfcManager.isNfcSupported(), isFalse);
      expect(
        logs,
        [isMethodCall('isNfcSupported', arguments: null)],
      );
    });
  });

  group('isNfcEnabled', () {
    test('returns true if method channel returns true.', () async {
      methodChannel.setMockResponse(logs, true);

      expect(await nfcManager.isNfcEnabled(), isTrue);
      expect(
        logs,
        [isMethodCall('isNfcEnabled', arguments: null)],
      );
    });

    test('returns true if method channel returns false.', () async {
      methodChannel.setMockResponse(logs, false);

      expect(await nfcManager.isNfcEnabled(), isFalse);
      expect(
        logs,
        [isMethodCall('isNfcEnabled', arguments: null)],
      );
    });
  });
}
