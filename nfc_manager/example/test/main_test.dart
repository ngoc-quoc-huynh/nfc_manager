import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/main.dart';

void main() {
  testWidgets('runs each check once, even when rebuilt.', (tester) async {
    final nfcManager = _StatusFake();
    await tester.pumpWidget(NfcManagerExample(nfcManager: nfcManager));
    await tester.pumpWidget(NfcManagerExample(nfcManager: nfcManager));

    expect(nfcManager.calls, 3);
  });
}

class _StatusFake extends NfcManager {
  int calls = 0;

  Future<bool> _check() {
    calls++;
    return Future.value(true);
  }

  @override
  Future<bool> isNfcSupported() => _check();

  @override
  Future<bool> isNfcEnabled() => _check();

  @override
  Future<bool> isHceSupported() => _check();
}
