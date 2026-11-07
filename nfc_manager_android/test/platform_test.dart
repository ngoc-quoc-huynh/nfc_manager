import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_android/nfc_manager_android.dart';

import 'utils.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  final originalPlatform = NfcManagerPlatform.instance;
  tearDown(() => NfcManagerPlatform.instance = originalPlatform);

  final nfcManager = NfcManagerAndroidPlatform();
  final methodChannel = nfcManager.methodChannel;

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
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockMethodCallHandler(methodChannel, (call) async {
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
    for (final milliseconds in <int?>[null, 3000]) {
      test(
        'passes timeout $milliseconds on subscription and cancels discovery.',
        () async {
          final listening = Completer<void>();
          final cancelled = Completer<void>();
          TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
              .setMockStreamHandler(
                nfcManager.discoveryEventChannel,
                MockStreamHandler.inline(
                  onListen: (arguments, events) {
                    expect(arguments, {'timeout': milliseconds});
                    listening.complete();
                    events.success('A1');
                  },
                  onCancel: (arguments) {
                    expect(arguments, {'timeout': milliseconds});
                    cancelled.complete();
                  },
                ),
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
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(methodChannel, (call) async {
              expect(call.method, 'sendCommand');
              expect(call.arguments, {
                'command': Uint8List.fromList([
                  0,
                  0xA4,
                  4,
                  0,
                  5,
                  0xF0,
                  0,
                  0,
                  0,
                  1,
                ]),
              });
              return Uint8List.fromList([0x90, 0]);
            });

        expect(
          await nfcManager.sendCommand(
            SelectAidCommand(Uint8List.fromList([0xF0, 0, 0, 0, 1])),
          ),
          ApduResponse.ok,
        );
      },
    );

    test('throws TagConnectionException for a null native response.', () async {
      methodChannel.setMockResponse(null);

      await expectLater(
        nfcManager.sendCommand(Command(cla: 0, ins: 0, p1: 0, p2: 0)),
        throwsA(isA<TagConnectionException>()),
      );
    });

    test(
      'converts native connection errors to TagConnectionException.',
      () async {
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(
              methodChannel,
              (_) async => throw PlatformException(
                code: 'TAG_CONNECTION_FAILED',
              ),
            );

        await expectLater(
          nfcManager.sendCommand(
            SelectAidCommand(Uint8List.fromList([0xF0, 0, 0, 0, 1])),
          ),
          throwsA(isA<TagConnectionException>()),
        );
      },
    );
  });

  group('startEmulation', () {
    test('preserves errors for unknown HCE event names.', () async {
      nfcManager.hostCardEmulationEventChannel.setMockStream(
        (events) => events
          ..success('UNKNOWN_EVENT')
          ..endOfStream(),
      );

      await expectLater(
        nfcManager.startEmulation(aid: Uint8List(5), pin: Uint8List(1)),
        emitsInOrder([emitsError(isA<ArgumentError>()), emitsDone]),
      );
    });

    test(
      'passes configuration on subscription and cancels the native stream.',
      () async {
        final aid = Uint8List.fromList([0xF0, 0, 0, 0, 1]);
        final pin = Uint8List.fromList([1]);
        final listening = Completer<void>();
        final cancelled = Completer<void>();
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockStreamHandler(
              nfcManager.hostCardEmulationEventChannel,
              MockStreamHandler.inline(
                onListen: (arguments, events) {
                  expect(arguments, {'aid': aid, 'pin': pin});
                  listening.complete();
                  events.success('READY');
                },
                onCancel: (arguments) {
                  expect(arguments, {'aid': aid, 'pin': pin});
                  cancelled.complete();
                },
              ),
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
