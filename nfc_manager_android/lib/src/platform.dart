import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

final class NfcManagerAndroid extends NfcManagerPlatform {
  final _methodChannel = const MethodChannel('dev.huynh/nfc_manager_android');

  static void registerWith() =>
      NfcManagerPlatform.instance = NfcManagerAndroid();

  @override
  Future<void> foo() => _methodChannel.invokeMethod<void>('foo');
}
