import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_android/nfc_manager_android.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

import 'utils.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  final originalPlatform = NfcManagerPlatform.instance;

  final nfcManager = NfcManagerAndroidPlatform();
  final methodChannel = nfcManager.methodChannel;

  tearDown(() {
    NfcManagerPlatform.instance = originalPlatform;
    methodChannel.clearMockCallHandler();
    nfcManager.discoveryEventChannel.clearMockStream();
    nfcManager.hostCardEmulationEventChannel.clearMockStream();
  });

  test('registers the Android platform implementation.', () {
    NfcManagerAndroidPlatform.registerWith();
    expect(NfcManagerPlatform.instance, isA<NfcManagerAndroidPlatform>());
  });

  for (final (method, check) in [
    ('isHceSupported', nfcManager.isHceSupported),
    ('isNfcSupported', nfcManager.isNfcSupported),
    ('isNfcEnabled', nfcManager.isNfcEnabled),
  ]) {
    group(method, () {
      for (final response in [true, false]) {
        test('calls $method and returns $response.', () async {
          methodChannel.setMockCallHandler((call) async {
            expect(call.method, method);
            expect(call.arguments, isNull);
            return response;
          });

          expect(await check(), response);
        });
      }
    });
  }

  group('startDiscovery', () {
    test('continues after a non-string discovery event.', () async {
      nfcManager.discoveryEventChannel.setMockStream(
        (events) => events
          ..success(42)
          ..success('A1')
          ..endOfStream(),
      );

      await expectLater(
        nfcManager.startDiscovery(),
        emitsInOrder([
          emitsError(const NfcUnknownException(code: 'INVALID_EVENT_PAYLOAD')),
          'A1',
          emitsDone,
        ]),
      );
    });

    for (final milliseconds in <int?>[null, 3000]) {
      test(
        'passes timeout $milliseconds on subscription and cancels discovery.',
        () async {
          final listening = Completer<void>();
          final cancelled = Completer<void>();
          nfcManager.discoveryEventChannel.setMockListener(
            (arguments, events) {
              expect(arguments, {'timeout': milliseconds});
              listening.complete();
              events.success('A1');
            },
            onCancel: (arguments) {
              expect(arguments, {'timeout': milliseconds});
              cancelled.complete();
            },
          );

          final stream = nfcManager.startDiscovery(
            timeout: milliseconds == null
                ? null
                : Duration(milliseconds: milliseconds),
          );
          expect(listening.isCompleted, isFalse);
          expect(await stream.first, 'A1');
          await cancelled.future;
        },
      );
    }

    test('emits tag IDs and converts native errors.', () async {
      nfcManager.discoveryEventChannel.setMockStream(
        (events) => events
          ..success('1')
          ..error(code: 'ISO_DEP_NOT_SUPPORTED')
          ..error(code: 'TAG_CONNECTION_FAILED')
          ..error(code: 'code')
          ..endOfStream(),
      );

      final stream = nfcManager.startDiscovery();
      await expectLater(
        stream,
        emitsInOrder([
          '1',
          emitsError(isA<IsoDepNotSupportedException>()),
          emitsError(isA<TagConnectionException>()),
          emitsError(isA<NfcUnknownException>()),
          emitsDone,
        ]),
      );
    });
  });

  group('sendCommand', () {
    test(
      'sends the serialized command using the existing wire format.',
      () async {
        methodChannel.setMockCallHandler((call) async {
          expect(call.method, 'sendCommand');
          expect(call.arguments, {
            'command': Uint8List.fromList([0, 0xA4, 4, 0, 5, 0xF0, 0, 0, 0, 1]),
          });
          return Uint8List.fromList([0x90, 0]);
        });

        final response = await nfcManager.sendCommand(
          ApduCommand.selectAid(Uint8List.fromList([0xF0, 0, 0, 0, 1])),
        );

        expect(response.data, isEmpty);
        expect(response.status, ApduStatus.success);
      },
    );

    test('keeps the response data and both status bytes.', () async {
      methodChannel.setMockResponse(
        Uint8List.fromList([0x6F, 0x02, 0x84, 0x00, 0x6C, 0x10]),
      );

      final response = await nfcManager.sendCommand(
        ApduCommand(cla: 0, ins: 0xB0, p1: 0, p2: 0, le: 0),
      );

      expect(response.data, [0x6F, 0x02, 0x84, 0x00]);
      expect(response.sw1, 0x6C);
      expect(response.sw2, 0x10);
      expect(response.status, ApduStatus.wrongLeField);
    });

    for (final bytes in <List<int>>[
      [],
      [0x90],
    ]) {
      test(
        'throws TagConnectionException for the response $bytes.',
        () async {
          methodChannel.setMockResponse(Uint8List.fromList(bytes));

          await expectLater(
            nfcManager.sendCommand(ApduCommand(cla: 0, ins: 0, p1: 0, p2: 0)),
            throwsA(const TagConnectionException()),
          );
        },
      );
    }

    test('throws TagConnectionException for a null native response.', () async {
      methodChannel.setMockResponse(null);

      await expectLater(
        nfcManager.sendCommand(ApduCommand(cla: 0, ins: 0, p1: 0, p2: 0)),
        throwsA(isA<TagConnectionException>()),
      );
    });

    test(
      'converts native connection errors to TagConnectionException.',
      () async {
        methodChannel.setMockCallHandler(
          (_) async => throw PlatformException(code: 'TAG_CONNECTION_FAILED'),
        );

        await expectLater(
          nfcManager.sendCommand(
            ApduCommand.selectAid(Uint8List.fromList([0xF0, 0, 0, 0, 1])),
          ),
          throwsA(isA<TagConnectionException>()),
        );
      },
    );
  });

  group('native errors', () {
    for (final (code, message, expected) in <(String, String?, NfcException)>[
      ('INVALID_APDU_COMMAND', null, const InvalidApduCommandException()),
      ('ISO_DEP_NOT_SUPPORTED', null, const IsoDepNotSupportedException()),
      ('NFC_NOT_SUPPORTED', null, const NfcNotSupportedException()),
      ('TAG_CONNECTION_FAILED', null, const TagConnectionException()),
      ('', null, const NfcUnknownException(code: '')),
      (
        'INVALID_ARGUMENTS',
        'native text',
        const NfcUnknownException(
          code: 'INVALID_ARGUMENTS',
          message: 'native text',
        ),
      ),
      (
        'INVALID_LC_DATA_LENGTH',
        'native text',
        const NfcUnknownException(
          code: 'INVALID_LC_DATA_LENGTH',
          message: 'native text',
        ),
      ),
      (
        'NFC_ERROR',
        'native text',
        const NfcUnknownException(code: 'NFC_ERROR', message: 'native text'),
      ),
      (
        'UNKNOWN',
        'native text',
        const NfcUnknownException(code: 'UNKNOWN', message: 'native text'),
      ),
    ]) {
      test('sendCommand maps "$code" to $expected.', () async {
        methodChannel.setMockCallHandler(
          (_) async => throw PlatformException(code: code, message: message),
        );

        await expectLater(
          nfcManager.sendCommand(ApduCommand(cla: 0, ins: 0, p1: 0, p2: 0)),
          throwsA(expected),
        );
      });

      test('startDiscovery maps "$code" to $expected.', () async {
        nfcManager.discoveryEventChannel.setMockStream(
          (events) => events
            ..error(code: code, message: message)
            ..endOfStream(),
        );

        await expectLater(
          nfcManager.startDiscovery(),
          emitsInOrder([emitsError(expected), emitsDone]),
        );
      });

      test('startEmulation maps "$code" to $expected.', () async {
        nfcManager.hostCardEmulationEventChannel.setMockStream(
          (events) => events
            ..error(code: code, message: message)
            ..endOfStream(),
        );

        await expectLater(
          nfcManager.startEmulation(aid: Uint8List(5), pin: Uint8List(1)),
          emitsInOrder([emitsError(expected), emitsDone]),
        );
      });
    }
  });

  test('keeps an unknown native code on NfcUnknownException.', () async {
    methodChannel.setMockCallHandler(
      (_) async => throw PlatformException(code: 'NEW_CODE', message: 'text'),
    );

    await expectLater(
      nfcManager.sendCommand(ApduCommand(cla: 0, ins: 0, p1: 0, p2: 0)),
      throwsA(
        isA<NfcUnknownException>()
            .having((e) => e.code, 'code', 'NEW_CODE')
            .having((e) => e.message, 'message', 'text'),
      ),
    );
  });

  group('startEmulation', () {
    for (final (name, status) in [
      ('READY', HostCardEmulationStatus.ready),
      ('INVALID_COMMAND', HostCardEmulationStatus.invalidCommand),
      ('INVALID_LC_LENGTH', HostCardEmulationStatus.invalidLcLength),
      ('AID_SELECTED', HostCardEmulationStatus.aidSelected),
      ('INVALID_AID', HostCardEmulationStatus.invalidAid),
      ('PIN_VERIFIED', HostCardEmulationStatus.pinVerified),
      ('WRONG_PIN', HostCardEmulationStatus.wrongPin),
      ('FUNCTION_NOT_SUPPORTED', HostCardEmulationStatus.functionNotSupported),
      ('TAG_DISCONNECTED', HostCardEmulationStatus.tagDisconnected),
    ]) {
      test('maps the native event $name to $status.', () async {
        nfcManager.hostCardEmulationEventChannel.setMockStream(
          (events) => events
            ..success(name)
            ..endOfStream(),
        );

        await expectLater(
          nfcManager.startEmulation(aid: Uint8List(5), pin: Uint8List(1)),
          emitsInOrder([status, emitsDone]),
        );
      });
    }

    test(
      'reports an unknown HCE event name as NfcUnknownException and continues.',
      () async {
        nfcManager.hostCardEmulationEventChannel.setMockStream(
          (events) => events
            ..success('UNKNOWN_EVENT')
            ..success('READY')
            ..endOfStream(),
        );

        await expectLater(
          nfcManager.startEmulation(aid: Uint8List(5), pin: Uint8List(1)),
          emitsInOrder([
            emitsError(
              const NfcUnknownException(
                code: 'UNKNOWN_HCE_STATUS',
                message: 'UNKNOWN_EVENT',
              ),
            ),
            HostCardEmulationStatus.ready,
            emitsDone,
          ]),
        );
      },
    );

    for (final payload in <Object?>[null, 42, <String, Object>{}]) {
      test('continues after a non-string HCE event: $payload.', () async {
        nfcManager.hostCardEmulationEventChannel.setMockStream(
          (events) => events
            ..success(payload)
            ..success('READY')
            ..endOfStream(),
        );

        await expectLater(
          nfcManager.startEmulation(aid: Uint8List(5), pin: Uint8List(1)),
          emitsInOrder([
            emitsError(
              const NfcUnknownException(code: 'INVALID_EVENT_PAYLOAD'),
            ),
            HostCardEmulationStatus.ready,
            emitsDone,
          ]),
        );
      });
    }

    test(
      'passes configuration on subscription and cancels the native stream.',
      () async {
        final aid = Uint8List.fromList([0xF0, 0, 0, 0, 1]);
        final pin = Uint8List.fromList([1]);
        final listening = Completer<void>();
        final cancelled = Completer<void>();
        nfcManager.hostCardEmulationEventChannel.setMockListener(
          (arguments, events) {
            expect(arguments, {'aid': aid, 'pin': pin});
            listening.complete();
            events.success('READY');
          },
          onCancel: (arguments) {
            expect(arguments, {'aid': aid, 'pin': pin});
            cancelled.complete();
          },
        );

        final stream = nfcManager.startEmulation(aid: aid, pin: pin);
        expect(listening.isCompleted, isFalse);
        expect(await stream.first, HostCardEmulationStatus.ready);
        await cancelled.future;
      },
    );

    test('emits HCE statuses and converts native errors.', () async {
      nfcManager.hostCardEmulationEventChannel.setMockStream(
        (events) => events
          ..success('READY')
          ..error(code: 'code')
          ..endOfStream(),
      );

      final stream = nfcManager.startEmulation(
        aid: Uint8List.fromList([0xF0, 0, 0, 0, 1]),
        pin: Uint8List.fromList([1]),
      );
      await expectLater(
        stream,
        emitsInOrder([
          HostCardEmulationStatus.ready,
          emitsError(isA<NfcUnknownException>()),
          emitsDone,
        ]),
      );
    });
  });
}
