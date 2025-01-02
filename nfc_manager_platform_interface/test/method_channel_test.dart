import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

import 'utils.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  final nfcManager = MethodChannelNfcManager();
  final methodChannel = nfcManager.methodChannel;

  group('isHceSupported', () {
    test('returns true if method channel returns true.', () async {
      methodChannel.setMockResponse(true);

      expect(await nfcManager.isHceSupported(), isTrue);
    });

    test('returns false if method channel returns false.', () async {
      methodChannel.setMockResponse(false);

      expect(await nfcManager.isHceSupported(), isFalse);
    });
  });

  group('isNfcSupported', () {
    test('returns true if method channel returns true.', () async {
      methodChannel.setMockResponse(true);

      expect(await nfcManager.isNfcSupported(), isTrue);
    });

    test('returns false if method channel returns false.', () async {
      methodChannel.setMockResponse(false);

      expect(await nfcManager.isNfcSupported(), isFalse);
    });
  });

  group('isNfcEnabled', () {
    test('returns true if method channel returns true.', () async {
      methodChannel.setMockResponse(true);

      expect(await nfcManager.isNfcEnabled(), isTrue);
    });

    test('returns false if method channel returns false.', () async {
      methodChannel.setMockResponse(false);

      expect(await nfcManager.isNfcEnabled(), isFalse);
    });
  });

  group('startDiscovery', () {
    test('emits correctly.', () async {
      nfcManager.discoveryEventChannel.setMockStream(
        (events) => events
          ..success('1')
          ..error(code: 'code')
          ..endOfStream(),
      );

      final stream = nfcManager.startDiscovery();
      await expectLater(
        stream,
        emitsInOrder(
          [
            '1',
            emitsError(isA<NfcUnknownException>()),
            emitsDone,
          ],
        ),
      );
    });
  });

  group('sendCommand', () {
    test('returns correctly', () async {
      methodChannel.setMockResponse([0x90, 0x00]);

      expect(
        await nfcManager.sendCommand(SelectAidCommand(Uint8List(0))),
        ApduResponse.ok,
      );
    });

    test('throws NfcException if PlatformException is thrown.', () async {
      methodChannel
          .setMockResponse(() => throw PlatformException(code: 'code'));

      await expectLater(
        nfcManager.sendCommand(SelectAidCommand(Uint8List(0))),
        throwsA(isA<NfcException>()),
      );
    });
  });

  group('startEmulation', () {
    test('emits correctly.', () async {
      nfcManager.hostCardEmulationEventChannel.setMockStream(
        (events) => events
          ..success('READY')
          ..error(code: 'code')
          ..endOfStream(),
      );

      final stream = nfcManager.startEmulation(
        aid: Uint8List(0),
        pin: Uint8List(0),
      );
      await expectLater(
        stream,
        emitsInOrder(
          [
            HostCardEmulationStatus.ready,
            emitsError(isA<NfcUnknownException>()),
            emitsDone,
          ],
        ),
      );
    });
  });
}
