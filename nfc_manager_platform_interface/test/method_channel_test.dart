import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  final nfcManager = MethodChannelNfcManager();
  final List<MethodCall> log = <MethodCall>[];

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      nfcManager.methodChannel,
      (methodCall) {
        log.add(methodCall);
        return null;
      },
    );

    log.clear();
  });

  test('foo calls method correctly.', () async {
    await nfcManager.foo();
    expect(
      log,
      [
        isMethodCall('foo', arguments: null),
      ],
    );
  });
}
