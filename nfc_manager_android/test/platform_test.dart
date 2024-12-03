import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_android/nfc_manager_android.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

import 'utils.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const methodChannel = NfcManagerAndroidPlatform.methodChannel;
  const discoveryEventChannel = NfcManagerAndroidPlatform.discoveryEventChannel;
  final logs = <MethodCall>[];

  tearDown(logs.clear);

  test('registers instance correctly.', () {
    NfcManagerAndroidPlatform.registerWith();
    expect(NfcManagerPlatform.instance, isA<NfcManagerAndroidPlatform>());
  });

  group('isHceSupported', () {
    test('returns true if method channel returns true.', () async {
      methodChannel.setMockResponse(logs, true);
      expect(await NfcManagerAndroidPlatform.test().isHceSupported(), isTrue);
      expect(
        logs,
        [isMethodCall('isHceSupported', arguments: null)],
      );
    });

    test('returns true if method channel returns false.', () async {
      methodChannel.setMockResponse(logs, false);

      expect(await NfcManagerAndroidPlatform.test().isHceSupported(), isFalse);
      expect(
        logs,
        [isMethodCall('isHceSupported', arguments: null)],
      );
    });
  });

  group('isNfcSupported', () {
    test('returns true if method channel returns true.', () async {
      methodChannel.setMockResponse(logs, true);

      expect(await NfcManagerAndroidPlatform.test().isNfcSupported(), isTrue);
      expect(
        logs,
        [isMethodCall('isNfcSupported', arguments: null)],
      );
    });

    test('returns true if method channel returns false.', () async {
      methodChannel.setMockResponse(logs, false);

      expect(await NfcManagerAndroidPlatform.test().isNfcSupported(), isFalse);
      expect(
        logs,
        [isMethodCall('isNfcSupported', arguments: null)],
      );
    });
  });

  group('isNfcEnabled', () {
    test('returns true if method channel returns true.', () async {
      methodChannel.setMockResponse(logs, true);

      expect(await NfcManagerAndroidPlatform.test().isNfcEnabled(), isTrue);
      expect(
        logs,
        [isMethodCall('isNfcEnabled', arguments: null)],
      );
    });

    test('returns true if method channel returns false.', () async {
      methodChannel.setMockResponse(logs, false);

      expect(await NfcManagerAndroidPlatform.test().isNfcEnabled(), isFalse);
      expect(
        logs,
        [isMethodCall('isNfcEnabled', arguments: null)],
      );
    });
  });

  group('startDiscovery', () {
    test('returns stream correctly.', () async {
      methodChannel.setMockResponse(logs, null);
      discoveryEventChannel.setMockStream((sink) => sink.success('1'));

      await expectLater(
        NfcManagerAndroidPlatform.test().startDiscovery(),
        emitsInOrder(['1', emitsDone]),
      );
      expect(
        logs,
        [isMethodCall('startDiscovery', arguments: null)],
      );
    });

    test(
      'returns correctly when called multiple times.',
      () async {
        final platform = NfcManagerAndroidPlatform.test()
          ..isDiscoveryStarted = true;
        discoveryEventChannel.setMockStream((sink) => sink.success('1'));

        await expectLater(
          platform.startDiscovery(),
          emitsInOrder(['1', emitsDone]),
        );
        expect(logs, <MethodCall>[]);
      },
    );

    test(
      'throws NfcNotSupportedException if error code is NFC_NOT_SUPPORTED,',
      () {
        methodChannel.setMockResponse(
          logs,
          PlatformException(
            code: 'NFC_NOT_SUPPORTED',
            message: 'message',
          ),
        );

        discoveryEventChannel.setMockStream((sink) => sink.success('1'));

        expect(
          // ignore: discarded_futures, this have to be unawaited.
          () => NfcManagerAndroidPlatform.test().startDiscovery().toList(),
          throwsA(isA<NfcNotSupportedException>()),
        );
        expect(logs, <MethodCall>[]);
      },
    );
  });

  group('stopDiscovery', () {
    test('returns correctly.', () async {
      methodChannel.setMockResponse(logs, null);
      await expectLater(
        NfcManagerAndroidPlatform.test().stopDiscovery(),
        completes,
      );

      expect(
        logs,
        [isMethodCall('stopDiscovery', arguments: null)],
      );
    });

    test(
      'throws NfcNotSupportedException if error code is NFC_NOT_SUPPORTED,',
      () {
        methodChannel.setMockResponse(
          logs,
          PlatformException(
            code: 'NFC_NOT_SUPPORTED',
            message: 'message',
          ),
        );

        discoveryEventChannel.setMockStream((sink) => sink.success('1'));

        expect(
          // ignore: discarded_futures, this have to have unawaited.
          () => NfcManagerAndroidPlatform.test().stopDiscovery(),
          throwsA(isA<NfcNotSupportedException>()),
        );
        expect(
          logs,
          [isMethodCall('stopDiscovery', arguments: null)],
        );
      },
    );
  });
}
