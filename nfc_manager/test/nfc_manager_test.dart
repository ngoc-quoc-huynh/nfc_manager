import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

// ignore_for_file: discarded_futures: mocked methods have to be async.

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
    test('can be created as a compile-time constant.', () {
      const first = NfcManager();
      const second = NfcManager();

      expect(identical(first, second), isTrue);
    });

    test('separate non-const calls create distinct objects.', () {
      const createManager = NfcManager.new;
      final first = createManager();
      final second = createManager();

      expect(identical(first, second), isFalse);
    });

    test('can be replaced by a mocktail mock.', () async {
      final NfcManager manager = MockNfcManager();
      when(manager.isNfcSupported).thenAnswer((_) async => true);

      expect(await manager.isNfcSupported(), isTrue);
      verify(manager.isNfcSupported).called(1);
    });
  });

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
      final error = _platformError(
        code: 'IS_HCE_SUPPORTED',
        message: 'HCE support lookup failed.',
      );
      when(
        mockPlatform.isHceSupported,
      ).thenAnswer((_) async => throw error);

      await expectLater(
        nfcManager.isHceSupported(),
        throwsA(same(error)),
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
      final error = _platformError(
        code: 'IS_NFC_SUPPORTED',
        message: 'NFC support lookup failed.',
      );
      when(
        mockPlatform.isNfcSupported,
      ).thenAnswer((_) async => throw error);

      await expectLater(
        nfcManager.isNfcSupported(),
        throwsA(same(error)),
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
      final error = _platformError(
        code: 'IS_NFC_ENABLED',
        message: 'NFC enabled lookup failed.',
      );
      when(
        mockPlatform.isNfcEnabled,
      ).thenAnswer((_) async => throw error);

      await expectLater(
        nfcManager.isNfcEnabled(),
        throwsA(same(error)),
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

    for (final (description, timeout) in [
      ('1 ms', const Duration(milliseconds: 1)),
      ('3 s', const Duration(seconds: 3)),
      ('2147483647 ms', const Duration(milliseconds: 0x7FFFFFFF)),
    ]) {
      test('forwards a timeout of $description.', () async {
        when(
          () => mockPlatform.startDiscovery(timeout: timeout),
        ).thenAnswer((_) => Stream.value('1'));

        await expectLater(
          nfcManager.startDiscovery(timeout: timeout),
          emitsInOrder(['1', emitsDone]),
        );
        verify(() => mockPlatform.startDiscovery(timeout: timeout)).called(1);
      });
    }

    test('forwards platform tag IDs.', () async {
      when(mockPlatform.startDiscovery).thenAnswer((_) => Stream.value('1'));

      await expectLater(
        nfcManager.startDiscovery(),
        emitsInOrder(['1', emitsDone]),
      );
    });

    test('forwards platform stream errors.', () async {
      final error = _platformError(
        code: 'START_DISCOVERY',
        message: 'Discovery failed.',
      );
      when(
        mockPlatform.startDiscovery,
      ).thenAnswer(
        (_) => Stream.error(error),
      );

      await expectLater(
        nfcManager.startDiscovery(),
        emitsInOrder([emitsError(same(error)), emitsDone]),
      );
    });
  });

  group('sendCommand', () {
    test('forwards the command and returns the platform response.', () async {
      final response = ApduResponse.fromUint8List(
        Uint8List.fromList([0x6F, 0x00, 0x90, 0x00]),
      );
      when(
        () => mockPlatform.sendCommand(command),
      ).thenAnswer((_) async => response);

      expect(await nfcManager.sendCommand(command), same(response));
    });

    test('forwards platform command errors.', () async {
      final error = _platformError(
        code: 'SEND_COMMAND',
        message: 'Command failed.',
      );
      when(
        () => mockPlatform.sendCommand(command),
      ).thenAnswer((_) async => throw error);

      await expectLater(
        nfcManager.sendCommand(command),
        throwsA(same(error)),
      );
    });
  });

  group('default platform', () {
    setUp(() => NfcManagerPlatform.instance = defaultPlatform);

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
      final error = _platformError(
        code: 'START_EMULATION',
        message: 'Emulation failed.',
      );
      when(
        () => mockPlatform.startEmulation(aid: aid, pin: pin),
      ).thenAnswer(
        (_) => Stream.error(error),
      );

      await expectLater(
        nfcManager.startEmulation(aid: aid, pin: pin),
        emitsInOrder([emitsError(same(error)), emitsDone]),
      );
    });
  });
}

final class MockNfcManager extends Mock implements NfcManager {}

NfcUnknownException _platformError({
  required String code,
  required String message,
}) => NfcUnknownException(code: code, message: message);

final class MockNfcManagerPlatform extends Mock
    with MockPlatformInterfaceMixin
    implements NfcManagerPlatform {}
