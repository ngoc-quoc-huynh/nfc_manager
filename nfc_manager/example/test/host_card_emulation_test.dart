import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/host_card_emulation.dart';

void main() {
  testWidgets('Shows readiness and cancels the stream on Stop.', (
    tester,
  ) async {
    final statuses = StreamController<HostCardEmulationStatus>.broadcast();
    await _pumpPage(tester, () => statuses.stream);

    await tester.tap(find.text('Start emulation'));
    await tester.pump();
    expect(find.byType(CircularProgressIndicator), findsOneWidget);

    statuses.add(.ready);
    await tester.pump();
    expect(find.text('HostCardEmulationStatus.ready'), findsOneWidget);

    await tester.tap(find.text('Stop emulation'));
    await tester.pump();
    expect(statuses.hasListener, isFalse);
    expect(find.text('Start emulation'), findsOneWidget);
    unawaited(statuses.close());
  });

  testWidgets('Keeps a terminal error visible until the user restarts.', (
    tester,
  ) async {
    final first = StreamController<HostCardEmulationStatus>.broadcast();
    final second = StreamController<HostCardEmulationStatus>.broadcast();
    final sessions = [first.stream, second.stream].iterator;
    await _pumpPage(tester, () {
      expect(sessions.moveNext(), isTrue);
      return sessions.current;
    });

    await tester.tap(find.text('Start emulation'));
    await tester.pump();
    const error = NfcUnknownException(code: 'EMULATION_FAILED');
    first.addError(error);
    unawaited(first.close());
    await tester.pump();
    expect(find.text('Received error: $error'), findsOneWidget);
    expect(second.hasListener, isFalse);

    await tester.tap(find.text('Restart emulation'));
    await tester.pump();
    expect(find.text('Received error: $error'), findsNothing);
    second.add(.ready);
    await tester.pump();
    expect(find.text('HostCardEmulationStatus.ready'), findsOneWidget);
    await tester.tap(find.text('Stop emulation'));
    await tester.pump();
    unawaited(second.close());
  });

  testWidgets('Shows a synchronous start failure and offers Restart.', (
    tester,
  ) async {
    final error = UnimplementedError('HCE is unavailable on this platform.');
    await _pumpPage(tester, () => throw error);

    await tester.tap(find.text('Start emulation'));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    expect(find.text('Received error: $error'), findsOneWidget);
    expect(find.text('Restart emulation'), findsOneWidget);
  });
}

Future<void> _pumpPage(
  WidgetTester tester,
  Stream<HostCardEmulationStatus> Function() start,
) => tester.pumpWidget(
  MaterialApp(home: HostCardEmulationPage(nfcManager: _EmulationFake(start))),
);

class _EmulationFake(
  final Stream<HostCardEmulationStatus> Function() start,
) extends NfcManager {
  @override
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) => start();
}
