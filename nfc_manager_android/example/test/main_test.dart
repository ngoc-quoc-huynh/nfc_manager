import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:nfc_manager_android_example/main.dart' as example;

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('dev.huynh/nfc_manager_android');
  const discovery = MethodChannel('dev.huynh/nfc_manager_android/discovery');
  const discoveryStream = EventChannel(
    'dev.huynh/nfc_manager_android/discovery',
  );
  const emulation = MethodChannel(
    'dev.huynh/nfc_manager_android/host_card_emulation',
  );
  const emulationStream = EventChannel(
    'dev.huynh/nfc_manager_android/host_card_emulation',
  );
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  tearDown(() {
    for (final channel in [channel, discovery, emulation]) {
      messenger.setMockMethodCallHandler(channel, null);
    }
  });

  testWidgets('shows errors from failed feature checks.', (tester) async {
    messenger.setMockMethodCallHandler(channel, (_) async {
      throw PlatformException(code: 'TEST_ERROR');
    });

    await tester.pumpWidget(const MaterialApp(home: example.NfcExample()));
    await tester.pump();

    expect(find.text('Could not complete the NFC checks.'), findsOneWidget);
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets(
    'sends a command before cancelling discovery after finding a tag.',
    (
      tester,
    ) async {
      final calls = <String>[];
      messenger
        ..setMockMethodCallHandler(channel, (call) async {
          calls.add(call.method);
          if (call.method == 'sendCommand') {
            return Uint8List.fromList([0x90, 0x00]);
          }
          return true;
        })
        ..setMockStreamHandler(
          discoveryStream,
          MockStreamHandler.inline(
            onListen: (_, events) {
              calls.add('discovery/listen');
              events.success('tag');
            },
            onCancel: (_) => calls.add('discovery/cancel'),
          ),
        )
        ..setMockStreamHandler(
          emulationStream,
          MockStreamHandler.inline(
            onListen: (_, events) {
              calls.add('host_card_emulation/listen');
              events.success('READY');
            },
            onCancel: (_) => calls.add('host_card_emulation/cancel'),
          ),
        );

      await tester.pumpWidget(const MaterialApp(home: example.NfcExample()));
      await tester.pump();
      await tester.runAsync(() => Future<void>.delayed(Duration.zero));
      await tester.pump();

      expect(
        calls,
        containsAllInOrder([
          'discovery/listen',
          'sendCommand',
          'discovery/cancel',
        ]),
      );
      expect(find.text('startDiscovery: tag'), findsOneWidget);
      expect(
        find.text(
          'sendCommand: ApduResponse(data: 0 bytes, '
          'statusWord: 9000, status: success)',
        ),
        findsOneWidget,
      );
      await tester.pumpWidget(const SizedBox.shrink());
    },
  );

  testWidgets('bounds and cancels discovery and emulation waits.', (
    tester,
  ) async {
    final methodCalls = <String>[];
    messenger.setMockMethodCallHandler(channel, (call) async {
      methodCalls.add(call.method);
      if (call.method == 'sendCommand') {
        return Uint8List.fromList([0x90, 0x00]);
      }
      return true;
    });
    final calls = <String>[];
    for (final channel in [discovery, emulation]) {
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add('${channel.name.split('/').last}/${call.method}');
        return null;
      });
    }

    await tester.pumpWidget(const MaterialApp(home: example.NfcExample()));
    await tester.pump(const Duration(seconds: 15));
    // Let the platform cancellation complete before advancing the HCE timer.
    await tester.runAsync(() => Future<void>.delayed(Duration.zero));
    await tester.pump();
    await tester.pump(const Duration(seconds: 15));
    await tester.runAsync(() => Future<void>.delayed(Duration.zero));
    await tester.pump();

    expect(find.text('NFC Manager Android'), findsOneWidget);
    expect(calls, [
      'discovery/listen',
      'discovery/cancel',
      'host_card_emulation/listen',
      'host_card_emulation/cancel',
    ], reason: 'Completed method calls: $methodCalls');
    expect(find.text('startDiscovery: timed out'), findsOneWidget);
    expect(find.text('startEmulation: timed out'), findsOneWidget);
    await tester.pumpWidget(const SizedBox.shrink());
  });
}
