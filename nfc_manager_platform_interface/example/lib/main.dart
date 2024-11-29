import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

final class NfcManagerExamplePlatform extends NfcManagerPlatform {
  final _methodChannel = const MethodChannel('dev.huynh/nfc_manager_example');

  static void registerWith() =>
      NfcManagerPlatform.instance = NfcManagerExamplePlatform();

  @override
  Future<void> foo() => _methodChannel.invokeMethod<void>('foo');
}
