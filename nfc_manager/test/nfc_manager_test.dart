import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:nfc_manager/nfc_manager.dart';

// ignore_for_file: discarded_futures: mocked methods have to be async.

void main() {
  final nfcManager = NfcManager();
  final mockPlatform = MockNfcManagerPlatform();

  setUpAll(
    () => NfcManagerPlatform.instance = mockPlatform,
  );

  tearDownAll(
    () => NfcManagerPlatform.instance = MethodChannelNfcManager(),
  );

  group('isHceSupported', () {
    test('returns true if platform returns true.', () async {
      when(mockPlatform.isHceSupported).thenAnswer(
        (_) async => true,
      );

      expect(await nfcManager.isHceSupported(), isTrue);
    });

    test('returns true if platform returns true.', () async {
      when(mockPlatform.isHceSupported).thenAnswer(
        (_) async => false,
      );

      expect(await nfcManager.isHceSupported(), isFalse);
    });
  });

  group('isNfcSupported', () {
    test('returns true if platform returns true.', () async {
      when(mockPlatform.isNfcSupported).thenAnswer(
        (_) async => true,
      );

      expect(await nfcManager.isNfcSupported(), isTrue);
    });

    test('returns true if platform returns true.', () async {
      when(mockPlatform.isNfcSupported).thenAnswer(
        (_) async => false,
      );

      expect(await nfcManager.isNfcSupported(), isFalse);
    });
  });

  group('isNfcEnabled', () {
    test('returns true if platform returns true.', () async {
      when(mockPlatform.isNfcEnabled).thenAnswer(
        (_) async => true,
      );

      expect(await nfcManager.isNfcEnabled(), isTrue);
    });

    test('returns true if platform returns true.', () async {
      when(mockPlatform.isNfcEnabled).thenAnswer(
        (_) async => false,
      );

      expect(await nfcManager.isNfcEnabled(), isFalse);
    });
  });

  group('startDiscovery', () {
    test('emits success correctly', () async {
      when(mockPlatform.startDiscovery).thenAnswer(
        (_) => Stream.value('1'),
      );

      await expectLater(
        nfcManager.startDiscovery(),
        emitsInOrder(
          [
            '1',
            emitsDone,
          ],
        ),
      );
    });

    test('emits error correctly.', () async {
      when(mockPlatform.startDiscovery).thenAnswer(
        (_) => Stream.error(const NfcUnknownException(null)),
      );

      await expectLater(
        nfcManager.startDiscovery(),
        emitsInOrder(
          [
            emitsError(isA<NfcUnknownException>()),
            emitsDone,
          ],
        ),
      );
    });
  });

  group('sendCommand', () {
    test('returns correctly.', () async {
      when(
        () => mockPlatform.sendCommand(SelectAidCommand(Uint8List(0))),
      ).thenAnswer(
        (_) async => ApduResponse.ok,
      );

      expect(
        await nfcManager.sendCommand(SelectAidCommand(Uint8List(0))),
        ApduResponse.ok,
      );
    });

    test('throws correctly.', () async {
      when(
        () => mockPlatform.sendCommand(SelectAidCommand(Uint8List(0))),
      ).thenThrow(const NfcUnknownException(null));

      await expectLater(
        () => nfcManager.sendCommand(SelectAidCommand(Uint8List(0))),
        throwsA(isA<NfcUnknownException>()),
      );
    });
  });

  group('startEmulation', () {
    final aid = Uint8List(0);
    final pin = Uint8List(0);
    test('emits success correctly', () async {
      when(
        () => mockPlatform.startEmulation(aid: aid, pin: pin),
      ).thenAnswer(
        (_) => Stream.value(HostCardEmulationStatus.ready),
      );

      await expectLater(
        nfcManager.startEmulation(aid: aid, pin: pin),
        emitsInOrder(
          [
            HostCardEmulationStatus.ready,
            emitsDone,
          ],
        ),
      );
    });

    test('emits error correctly.', () async {
      when(
        () => mockPlatform.startEmulation(aid: aid, pin: pin),
      ).thenAnswer(
        (_) => Stream.error(const NfcUnknownException(null)),
      );

      await expectLater(
        nfcManager.startEmulation(aid: aid, pin: pin),
        emitsInOrder(
          [
            emitsError(isA<NfcUnknownException>()),
            emitsDone,
          ],
        ),
      );
    });
  });
}

final class MockNfcManagerPlatform extends Mock
    with MockPlatformInterfaceMixin, MockNfcManagerPlatformMixin {}
