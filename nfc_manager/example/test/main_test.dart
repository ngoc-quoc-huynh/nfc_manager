import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/main.dart';

void main() {
  testWidgets('Shows pending checks, results and a compact error on rebuild.', (
    tester,
  ) async {
    tester.view
      ..devicePixelRatio = 1
      ..physicalSize = const Size(220, 600);
    addTearDown(tester.view.reset);
    final nfcManager = _StatusFake();

    await tester.pumpWidget(NfcManagerExample(nfcManager: nfcManager));
    expect(find.byType(CircularProgressIndicator), findsNWidgets(3));

    nfcManager.nfcSupported.complete(true);
    nfcManager.nfcEnabled.complete(false);
    nfcManager.hceSupported.completeError(
      StateError('A long status error message. ' * 20),
    );
    await tester.pump();

    expect(tester.takeException(), isNull);
    expect(find.byType(CircularProgressIndicator), findsNothing);
    expect(find.text('true'), findsOneWidget);
    expect(find.text('false'), findsOneWidget);
    expect(find.text('Check failed'), findsOneWidget);

    await tester.pumpWidget(NfcManagerExample(nfcManager: nfcManager));
    expect(nfcManager.checks, unorderedEquals(['nfc', 'enabled', 'hce']));
    expect(find.text('Check failed'), findsOneWidget);
  });
}

class _StatusFake extends NfcManager {
  final nfcSupported = Completer<bool>();
  final nfcEnabled = Completer<bool>();
  final hceSupported = Completer<bool>();
  final checks = <String>[];

  @override
  Future<bool> isNfcSupported() {
    checks.add('nfc');
    return nfcSupported.future;
  }

  @override
  Future<bool> isNfcEnabled() {
    checks.add('enabled');
    return nfcEnabled.future;
  }

  @override
  Future<bool> isHceSupported() {
    checks.add('hce');
    return hceSupported.future;
  }
}
