import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:nfc_manager/nfc_manager.dart';

// ignore_for_file: discarded_futures: mocked methods have to be async.

void main() {
  final originalPlatform = NfcManagerPlatform.instance;
  final nfcManager = NfcManager();
  final aid = Uint8List.fromList([0xF0, 0, 0, 0, 1]);
  final pin = Uint8List.fromList([1, 2, 3, 4]);
  final command = ApduCommand.selectAid(aid);
  late MockNfcManagerPlatform mockPlatform;

  setUpAll(() => registerFallbackValue(Uint8List(0)));

  setUp(() {
    mockPlatform = MockNfcManagerPlatform();
    NfcManagerPlatform.instance = mockPlatform;
  });

  tearDownAll(() => NfcManagerPlatform.instance = originalPlatform);

  group('isHceSupported', () {
    test('uses platform replacements after the first call.', () async {
      when(mockPlatform.isHceSupported).thenAnswer((_) async => false);
      expect(await nfcManager.isHceSupported(), isFalse);

      final replacement = MockNfcManagerPlatform();
      when(replacement.isHceSupported).thenAnswer((_) async => true);
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

    test('forwards platform errors.', () async {
      when(
        mockPlatform.isHceSupported,
      ).thenAnswer((_) async => throw const NfcUnknownException(null));

      await expectLater(
        nfcManager.isHceSupported(),
        throwsA(isA<NfcUnknownException>()),
      );
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

    test('forwards platform errors.', () async {
      when(
        mockPlatform.isNfcSupported,
      ).thenAnswer((_) async => throw const NfcUnknownException(null));

      await expectLater(
        nfcManager.isNfcSupported(),
        throwsA(isA<NfcUnknownException>()),
      );
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

    test('forwards platform errors.', () async {
      when(
        mockPlatform.isNfcEnabled,
      ).thenAnswer((_) async => throw const NfcUnknownException(null));

      await expectLater(
        nfcManager.isNfcEnabled(),
        throwsA(isA<NfcUnknownException>()),
      );
    });
  });

  group('startDiscovery', () {
    for (final (description, timeout) in [
      ('a zero', Duration.zero),
      ('a negative', const Duration(milliseconds: -1)),
      ('a sub-millisecond', const Duration(microseconds: 500)),
      ('a fractional millisecond', const Duration(microseconds: 1500)),
      ('an Int32 overflowing', const Duration(milliseconds: 0x80000000)),
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

    test('forwards the largest supported timeout.', () async {
      const timeout = Duration(milliseconds: 0x7FFFFFFF);
      when(
        () => mockPlatform.startDiscovery(timeout: timeout),
      ).thenAnswer((_) => Stream.value('1'));

      await expectLater(
        nfcManager.startDiscovery(timeout: timeout),
        emitsInOrder(['1', emitsDone]),
      );
      verify(() => mockPlatform.startDiscovery(timeout: timeout)).called(1);
    });

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
        () => mockPlatform.sendCommand(command),
      ).thenAnswer((_) async => ApduResponse.ok);

      expect(await nfcManager.sendCommand(command), ApduResponse.ok);
    });

    test('forwards platform command errors.', () async {
      when(
        () => mockPlatform.sendCommand(command),
      ).thenAnswer((_) async => throw const NfcUnknownException(null));

      await expectLater(
        nfcManager.sendCommand(command),
        throwsA(isA<NfcUnknownException>()),
      );
    });
  });

  group('default platform', () {
    setUp(() => NfcManagerPlatform.instance = originalPlatform);

    test('throws an UnimplementedError for every method.', () {
      final calls = <Object? Function()>[
        nfcManager.isHceSupported,
        nfcManager.isNfcSupported,
        nfcManager.isNfcEnabled,
        nfcManager.startDiscovery,
        () => nfcManager.sendCommand(command),
        () => nfcManager.startEmulation(aid: aid, pin: pin),
      ];

      for (final call in calls) {
        expect(call, throwsUnimplementedError);
      }
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
