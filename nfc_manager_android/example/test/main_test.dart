import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:nfc_manager_android_example/main.dart' as example;

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets(
    'sends a command before cancelling discovery and shows HCE readiness.',
    (tester) async {
      final calls = <String>[];
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        ..setMockMethodCallHandler(
          const MethodChannel('dev.huynh/nfc_manager_android'),
          (call) async {
            calls.add(call.method);
            return call.method == 'sendCommand'
                ? Uint8List.fromList([0x90, 0x00])
                : true;
          },
        )
        ..setMockStreamHandler(
          const EventChannel('dev.huynh/nfc_manager_android/discovery'),
          MockStreamHandler.inline(
            onListen: (_, events) {
              calls.add('discovery/listen');
              events.success('tag');
            },
            onCancel: (_) => calls.add('discovery/cancel'),
          ),
        )
        ..setMockStreamHandler(
          const EventChannel(
            'dev.huynh/nfc_manager_android/host_card_emulation',
          ),
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

      // Cancelling discovery closes the tag, so sendCommand must come first.
      expect(
        calls,
        containsAllInOrder([
          'discovery/listen',
          'sendCommand',
          'discovery/cancel',
          'host_card_emulation/listen',
          'host_card_emulation/cancel',
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
      expect(
        find.text('startEmulation: HostCardEmulationStatus.ready'),
        findsOneWidget,
      );
      await tester.pumpWidget(const SizedBox.shrink());
    },
  );
}
