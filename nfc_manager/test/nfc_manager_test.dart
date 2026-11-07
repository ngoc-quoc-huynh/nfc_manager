import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:nfc_manager/nfc_manager.dart';

// ignore_for_file: discarded_futures: mocked methods have to be async.

void main() {
  final originalPlatform = NfcManagerPlatform.instance;
  final nfcManager = NfcManager();
  final mockPlatform = MockNfcManagerPlatform();

  setUpAll(() => NfcManagerPlatform.instance = mockPlatform);

  tearDownAll(() => NfcManagerPlatform.instance = originalPlatform);

  group('isHceSupported', () {
    test('uses platform replacements after the first call.', () async {
      when(mockPlatform.isHceSupported).thenAnswer((_) async => false);
      expect(await nfcManager.isHceSupported(), isFalse);

      final replacement = MockNfcManagerPlatform();
      when(replacement.isHceSupported).thenAnswer((_) async => true);
      addTearDown(() => NfcManagerPlatform.instance = mockPlatform);
      NfcManagerPlatform.instance = replacement;

      expect(await nfcManager.isHceSupported(), isTrue);
    });

    test('returns true if the platform returns true.', () async {
      when(mockPlatform.isHceSupported).thenAnswer((_) async => true);

      expect(await nfcManager.isHceSupported(), isTrue);
    });

    test('returns false if the platform returns false.', () async {
      when(mockPlatform.isHceSupported).thenAnswer((_) async => false);

      expect(await nfcManager.isHceSupported(), isFalse);
    });
  });

  group('isNfcSupported', () {
    test('returns true if the platform returns true.', () async {
      when(mockPlatform.isNfcSupported).thenAnswer((_) async => true);

      expect(await nfcManager.isNfcSupported(), isTrue);
    });

    test('returns false if the platform returns false.', () async {
      when(mockPlatform.isNfcSupported).thenAnswer((_) async => false);

      expect(await nfcManager.isNfcSupported(), isFalse);
    });
  });

  group('isNfcEnabled', () {
    test('returns true if the platform returns true.', () async {
      when(mockPlatform.isNfcEnabled).thenAnswer((_) async => true);

      expect(await nfcManager.isNfcEnabled(), isTrue);
    });

    test('returns false if the platform returns false.', () async {
      when(mockPlatform.isNfcEnabled).thenAnswer((_) async => false);

      expect(await nfcManager.isNfcEnabled(), isFalse);
    });
  });

  group('startDiscovery', () {
    test('forwards the supplied communication timeout.', () async {
      const timeout = Duration(seconds: 3);
      when(
        () => mockPlatform.startDiscovery(timeout: timeout),
      ).thenAnswer((_) => Stream.value('1'));

      await expectLater(
        nfcManager.startDiscovery(timeout: timeout),
        emitsInOrder(['1', emitsDone]),
      );
      verify(() => mockPlatform.startDiscovery(timeout: timeout)).called(1);
    });

    test('forwards platform tag IDs.', () async {
      when(mockPlatform.startDiscovery).thenAnswer((_) => Stream.value('1'));

      await expectLater(
        nfcManager.startDiscovery(),
        emitsInOrder(['1', emitsDone]),
      );
    });

    test('forwards platform stream errors.', () async {
      when(
        mockPlatform.startDiscovery,
      ).thenAnswer((_) => Stream.error(const NfcUnknownException(null)));

      await expectLater(
        nfcManager.startDiscovery(),
        emitsInOrder([emitsError(isA<NfcUnknownException>()), emitsDone]),
      );
    });
  });

  group('sendCommand', () {
    test('forwards the command and returns the platform response.', () async {
      when(
        () => mockPlatform.sendCommand(
          SelectAidCommand(Uint8List.fromList([0xF0, 0, 0, 0, 1])),
        ),
      ).thenAnswer((_) async => ApduResponse.ok);

      expect(
        await nfcManager.sendCommand(
          SelectAidCommand(Uint8List.fromList([0xF0, 0, 0, 0, 1])),
        ),
        ApduResponse.ok,
      );
    });

    test('forwards platform command errors.', () async {
      when(
        () => mockPlatform.sendCommand(
          SelectAidCommand(Uint8List.fromList([0xF0, 0, 0, 0, 1])),
        ),
      ).thenAnswer((_) async => throw const NfcUnknownException(null));

      await expectLater(
        nfcManager.sendCommand(
          SelectAidCommand(Uint8List.fromList([0xF0, 0, 0, 0, 1])),
        ),
        throwsA(isA<NfcUnknownException>()),
      );
    });
  });

  group('startEmulation', () {
    final aid = Uint8List(0);
    final pin = Uint8List(0);
    test('forwards configuration and platform HCE events.', () async {
      when(
        () => mockPlatform.startEmulation(aid: aid, pin: pin),
      ).thenAnswer((_) => Stream.value(HostCardEmulationStatus.ready));

      await expectLater(
        nfcManager.startEmulation(aid: aid, pin: pin),
        emitsInOrder([HostCardEmulationStatus.ready, emitsDone]),
      );
    });

    test('forwards platform stream errors.', () async {
      when(
        () => mockPlatform.startEmulation(aid: aid, pin: pin),
      ).thenAnswer((_) => Stream.error(const NfcUnknownException(null)));

      await expectLater(
        nfcManager.startEmulation(aid: aid, pin: pin),
        emitsInOrder([emitsError(isA<NfcUnknownException>()), emitsDone]),
      );
    });
  });
}

final class MockNfcManagerPlatform extends Mock
    with MockPlatformInterfaceMixin
    implements NfcManagerPlatform {}
