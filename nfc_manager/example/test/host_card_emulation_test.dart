import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/host_card_emulation.dart';

void main() {
  testWidgets('shows readiness and cancels the stream on Stop.', (
    tester,
  ) async {
    final statuses = StreamController<HostCardEmulationStatus>.broadcast();
    addTearDown(() => unawaited(statuses.close()));
    await _pumpPage(tester, () => statuses.stream);

    await tester.tap(find.text('Start emulation'));
    await tester.pump();
    expect(find.byType(CircularProgressIndicator), findsOneWidget);

    statuses.add(.ready);
    await tester.pump();
    expect(find.text('ready'), findsOneWidget);

    await tester.tap(find.text('Stop emulation'));
    await tester.pump();
    expect(statuses.hasListener, isFalse);
    expect(find.text('Start emulation'), findsOneWidget);
  });

  testWidgets('keeps PIN_BLOCKED visible until the user restarts.', (
    tester,
  ) async {
    final first = StreamController<HostCardEmulationStatus>.broadcast();
    final second = StreamController<HostCardEmulationStatus>.broadcast();
    addTearDown(() => unawaited(first.close()));
    addTearDown(() => unawaited(second.close()));
    final sessions = [first.stream, second.stream];
    await _pumpPage(tester, () => sessions.removeAt(0));

    await tester.tap(find.text('Start emulation'));
    await tester.pump();
    const blocked = 'PIN blocked after 3 wrong attempts. Restart to try again.';
    first.addError(
      const NfcUnknownException(code: 'PIN_BLOCKED', message: 'native detail'),
    );
    unawaited(first.close());
    await tester.pump();
    expect(find.text(blocked), findsOneWidget);
    expect(find.textContaining('native detail'), findsNothing);

    await tester.tap(find.text('Restart emulation'));
    await tester.pump();
    second.add(.ready);
    await tester.pump();
    expect(find.text('ready'), findsOneWidget);
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
