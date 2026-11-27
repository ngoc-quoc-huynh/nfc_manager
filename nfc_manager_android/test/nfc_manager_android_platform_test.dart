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

  tearDown(() {
    NfcManagerPlatform.instance = originalPlatform;
    clearMockHostApiHandlers();
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
          setMockHostApiHandler(method, (message) {
            expect(message, isNull);
            return [response];
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

    test('keeps emitting tag IDs after per-tag errors.', () async {
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
      'sends the command bytes as the only argument.',
      () async {
        setMockHostApiHandler('sendCommand', (message) {
          expect(message, [
            Uint8List.fromList([0, 0xA4, 4, 0, 5, 0xF0, 0, 0, 0, 1]),
          ]);
          return [
            Uint8List.fromList([0x90, 0]),
          ];
        });

        final response = await nfcManager.sendCommand(
          ApduCommand.selectAid(Uint8List.fromList([0xF0, 0, 0, 0, 1])),
        );

        expect(response.data, isEmpty);
        expect(response.status, ApduStatus.success);
      },
    );

    test('keeps the response data and both status bytes.', () async {
      setMockHostApiHandler(
        'sendCommand',
        (_) => [
          Uint8List.fromList([0x6F, 0x02, 0x84, 0x00, 0x6C, 0x10]),
        ],
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
          setMockHostApiHandler(
            'sendCommand',
            (_) => [Uint8List.fromList(bytes)],
          );

          await expectLater(
            nfcManager.sendCommand(ApduCommand(cla: 0, ins: 0, p1: 0, p2: 0)),
            throwsA(const TagConnectionException()),
          );
        },
      );
    }

    test(
      'throws NfcUnknownException with "channel-error" without the plugin.',
      () => expectLater(
        nfcManager.sendCommand(ApduCommand(cla: 0, ins: 0, p1: 0, p2: 0)),
        throwsA(
          isA<NfcUnknownException>().having(
            (e) => e.code,
            'code',
            'channel-error',
          ),
        ),
      ),
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
        setMockHostApiHandler(
          'sendCommand',
          (_) => [code, message, null],
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

    test(
      'does not check the AID itself and reports the native error.',
      () async {
        final aid = Uint8List(4);
        final pin = Uint8List(1);
        const message = 'native text';
        nfcManager.hostCardEmulationEventChannel.setMockListener((
          arguments,
          events,
        ) {
          expect(arguments, {'aid': aid, 'pin': pin});
          events
            ..error(code: 'INVALID_ARGUMENTS', message: message)
            ..endOfStream();
        });

        await expectLater(
          nfcManager.startEmulation(aid: aid, pin: pin),
          emitsInOrder([
            emitsError(
              isA<NfcUnknownException>()
                  .having((error) => error.code, 'code', 'INVALID_ARGUMENTS')
                  .having((error) => error.message, 'message', message),
            ),
            emitsDone,
          ]),
        );
      },
    );

    test(
      'PIN_BLOCKED ends the stream, and a new call starts a new session.',
      () async {
        var listenCount = 0;
        final aid = Uint8List.fromList([0xF0, 0, 0, 0, 1]);
        final pin = Uint8List.fromList([1, 2, 3, 4]);
        final cancelled = Completer<void>();
        nfcManager.hostCardEmulationEventChannel.setMockListener(
          (
            arguments,
            events,
          ) {
            expect(arguments, {'aid': aid, 'pin': pin});
            listenCount++;
            if (listenCount == 1) {
              events
                ..error(
                  code: 'PIN_BLOCKED',
                  message: 'Card emulation ended after too many wrong PINs.',
                )
                ..endOfStream();
            }
          },
          onCancel: (_) {
            if (!cancelled.isCompleted) {
              cancelled.complete();
            }
          },
        );

        final firstSession = nfcManager.startEmulation(aid: aid, pin: pin);
        await expectLater(
          firstSession,
          emitsInOrder([
            emitsError(
              isA<NfcUnknownException>()
                  .having((error) => error.code, 'code', 'PIN_BLOCKED')
                  .having(
                    (error) => error.message,
                    'message',
                    'Card emulation ended after too many wrong PINs.',
                  ),
            ),
            emitsDone,
          ]),
        );
        expect(listenCount, 1);
        await cancelled.future;

        nfcManager.hostCardEmulationEventChannel.setMockStream(
          (events) {
            listenCount++;
            events
              ..success('READY')
              ..endOfStream();
          },
        );
        await expectLater(
          nfcManager.startEmulation(aid: aid, pin: pin),
          emitsInOrder([HostCardEmulationStatus.ready, emitsDone]),
        );
        expect(listenCount, 2);
      },
    );
  });
}
