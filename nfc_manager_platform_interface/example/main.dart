import 'package:flutter/foundation.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

Future<void> main() async {
  ExampleNfcPlatform.registerWith();
  final isSupported = await NfcManagerPlatform.instance.isNfcSupported();
  debugPrint('NFC supported: $isSupported');
}

class ExampleNfcPlatform extends NfcManagerPlatform {
  static void registerWith() =>
      NfcManagerPlatform.instance = ExampleNfcPlatform();

  @override
  Future<bool> isNfcSupported() async => false;
}
