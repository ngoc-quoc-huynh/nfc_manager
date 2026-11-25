import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/tag_reader.dart';

void main() {
  testWidgets('shows the error code and offers Restart.', (tester) async {
    const error = NfcUnknownException(
      code: 'NFC_ERROR',
      message: 'native detail',
    );
    await tester.pumpWidget(
      MaterialApp(home: TagReaderPage(nfcManager: _ReaderFake(.error(error)))),
    );
    await tester.tap(find.text('Start discovery'));
    await tester.pumpAndSettle();

    expect(find.text('NFC failed: NFC_ERROR'), findsOneWidget);
    expect(find.textContaining('native detail'), findsNothing);
    expect(find.text('Restart discovery'), findsOneWidget);
  });
}

class _ReaderFake(final Stream<String> tags) extends NfcManager {
  @override
  Stream<String> startDiscovery({Duration? timeout}) => tags;
}
