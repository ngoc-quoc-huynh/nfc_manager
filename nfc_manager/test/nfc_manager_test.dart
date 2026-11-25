import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

void main() {
  final defaultPlatform = NfcManagerPlatform.instance;
  const nfcManager = NfcManager();
  final aid = Uint8List.fromList([0xF0, 0, 0, 0, 1]);
  final pin = Uint8List.fromList([1, 2, 3, 4]);
  final command = ApduCommand.selectAid(aid);
  late MockNfcManagerPlatform mockPlatform;

  setUpAll(() => registerFallbackValue(Uint8List(0)));

  setUp(() {
    mockPlatform = MockNfcManagerPlatform();
    NfcManagerPlatform.instance = mockPlatform;
  });

  tearDown(() => NfcManagerPlatform.instance = defaultPlatform);

  group('NfcManager', () {
    test('apps can mock NfcManager with mocktail.', () async {
      final NfcManager manager = MockNfcManager();
      when(manager.isNfcSupported).thenAnswer((_) async => true);

      expect(await manager.isNfcSupported(), isTrue);
      verify(manager.isNfcSupported).called(1);
    });

    test('uses the current platform instance on each call.', () async {
      when(mockPlatform.isHceSupported).thenAnswer((_) async => false);
      expect(await nfcManager.isHceSupported(), isFalse);

      final replacement = MockNfcManagerPlatform();
      when(replacement.isHceSupported).thenAnswer((_) async => true);
      NfcManagerPlatform.instance = replacement;

      expect(await nfcManager.isHceSupported(), isTrue);
    });
  });

  for (final (name, platformCall, call) in [
    (
      'isHceSupported',
      (NfcManagerPlatform p) => p.isHceSupported(),
      nfcManager.isHceSupported,
    ),
    (
      'isNfcSupported',
      (NfcManagerPlatform p) => p.isNfcSupported(),
      nfcManager.isNfcSupported,
    ),
    (
      'isNfcEnabled',
      (NfcManagerPlatform p) => p.isNfcEnabled(),
      nfcManager.isNfcEnabled,
    ),
  ]) {
    group(name, () {
      test('returns the platform result.', () async {
        for (final value in [true, false]) {
          when(() => platformCall(mockPlatform)).thenAnswer((_) async => value);

          expect(await call(), value, reason: '$value');
        }
      });

      test('forwards platform errors.', () async {
        when(
          () => platformCall(mockPlatform),
        ).thenAnswer((_) async => throw _error);

        await expectLater(call(), throwsA(same(_error)));
      });
    });
  }

  group('startDiscovery', () {
    for (final (description, timeout) in [
      ('a zero', Duration.zero),
      ('a negative', const Duration(milliseconds: -1)),
      ('a sub-millisecond', const Duration(microseconds: 500)),
      ('a fractional millisecond', const Duration(microseconds: 1500)),
      ('a too large', const Duration(milliseconds: 0x80000000)),
    ]) {
      test('throws an ArgumentError for $description timeout.', () {
        expect(
          () => nfcManager.startDiscovery(timeout: timeout),
          throwsA(
            isA<ArgumentError>()
                .having((error) => error.name, 'name', 'timeout')
                .having((error) => error.invalidValue, 'invalidValue', timeout),
          ),
        );
        verifyNever(
          () => mockPlatform.startDiscovery(timeout: any(named: 'timeout')),
        );
      });
    }

    for (final (description, timeout) in [
      ('no', null),
      ('1 ms', const Duration(milliseconds: 1)),
      ('3 s', const Duration(seconds: 3)),
      ('2147483647 ms', const Duration(milliseconds: 0x7FFFFFFF)),
    ]) {
      test('forwards $description timeout.', () async {
        when(
          () => mockPlatform.startDiscovery(timeout: timeout),
        ).thenAnswer((_) => Stream.value('1'));

        await expectLater(
          nfcManager.startDiscovery(timeout: timeout),
          emitsInOrder(['1', emitsDone]),
        );
      });
    }

    test('forwards platform stream errors.', () async {
      when(
        mockPlatform.startDiscovery,
      ).thenAnswer(
        (_) => Stream.error(_error),
      );

      await expectLater(
        nfcManager.startDiscovery(),
        emitsInOrder([emitsError(same(_error)), emitsDone]),
      );
    });
  });

  group('sendCommand', () {
    test('forwards the command and returns the platform response.', () async {
      final response = ApduResponse.fromUint8List(
        .fromList([0x6F, 0x00, 0x90, 0x00]),
      );
      when(
        () => mockPlatform.sendCommand(command),
      ).thenAnswer((_) async => response);

      expect(await nfcManager.sendCommand(command), same(response));
    });

    test('forwards platform command errors.', () async {
      when(
        () => mockPlatform.sendCommand(command),
      ).thenAnswer((_) async => throw _error);

      await expectLater(
        nfcManager.sendCommand(command),
        throwsA(same(_error)),
      );
    });
  });

  group('startEmulation', () {
    for (final (description, invalidAid, invalidPin, name) in [
      ('an AID shorter than 5 bytes', Uint8List(4), pin, 'aid.length'),
      ('an AID longer than 16 bytes', Uint8List(17), pin, 'aid.length'),
      ('an empty PIN', aid, Uint8List(0), 'pin.length'),
      ('a PIN longer than 255 bytes', aid, Uint8List(256), 'pin.length'),
    ]) {
      test('throws a RangeError for $description.', () {
        expect(
          () => nfcManager.startEmulation(aid: invalidAid, pin: invalidPin),
          throwsA(
            isA<RangeError>().having((error) => error.name, 'name', name),
          ),
        );
        verifyNever(
          () => mockPlatform.startEmulation(
            aid: any(named: 'aid'),
            pin: any(named: 'pin'),
          ),
        );
      });
    }

    for (final (aidLength, pinLength) in [(5, 1), (16, 255)]) {
      test(
        'forwards an AID of $aidLength bytes and a PIN of $pinLength bytes.',
        () async {
          final validAid = Uint8List(aidLength);
          final validPin = Uint8List(pinLength);
          when(
            () => mockPlatform.startEmulation(aid: validAid, pin: validPin),
          ).thenAnswer((_) => Stream.value(HostCardEmulationStatus.ready));

          await expectLater(
            nfcManager.startEmulation(aid: validAid, pin: validPin),
            emitsInOrder([HostCardEmulationStatus.ready, emitsDone]),
          );
        },
      );
    }

    test('forwards platform stream errors.', () async {
      when(
        () => mockPlatform.startEmulation(aid: aid, pin: pin),
      ).thenAnswer(
        (_) => Stream.error(_error),
      );

      await expectLater(
        nfcManager.startEmulation(aid: aid, pin: pin),
        emitsInOrder([emitsError(same(_error)), emitsDone]),
      );
    });
  });
}

final class MockNfcManager extends Mock implements NfcManager {}

const _error = NfcUnknownException(code: 'ERROR');

final class MockNfcManagerPlatform extends Mock
    with MockPlatformInterfaceMixin
    implements NfcManagerPlatform {}
