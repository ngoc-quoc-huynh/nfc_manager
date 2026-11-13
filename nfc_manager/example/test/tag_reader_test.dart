import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/tag_reader.dart';

void main() {
  testWidgets('Select AID dismisses the current snackbar early.', (
    tester,
  ) async {
    final messengerKey = GlobalKey<ScaffoldMessengerState>();
    final tags = StreamController<String>.broadcast();
    addTearDown(tags.close);
    final nfcManager = _ReaderFake(tags.stream);
    await tester.pumpWidget(
      MaterialApp(
        scaffoldMessengerKey: messengerKey,
        home: TagReaderPage(nfcManager: nfcManager),
      ),
    );
    await tester.tap(find.text('Start discovery'));
    await tester.pump();
    tags.add('0102');
    await tester.pumpAndSettle();

    messengerKey.currentState!.showSnackBar(
      const SnackBar(
        content: Text('Previous message'),
        duration: Duration(minutes: 1),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Previous message'), findsOneWidget);

    await tester.tap(find.text('Select aid'));
    final response = ApduResponse.fromUint8List(Uint8List.fromList([0x90, 0]));
    nfcManager.response.complete(response);
    await tester.pumpAndSettle();

    expect(find.text('Previous message'), findsNothing);
    expect(find.text('Select AID response: $response'), findsOneWidget);
  });
}

class _ReaderFake(final Stream<String> tags) extends NfcManager {
  final response = Completer<ApduResponse>();

  @override
  Stream<String> startDiscovery({Duration? timeout}) => tags;

  @override
  Future<ApduResponse> sendCommand(ApduCommand command) => response.future;
}
