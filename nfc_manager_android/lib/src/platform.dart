import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

base class NfcManagerAndroidPlatform extends NfcManagerPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('dev.huynh/nfc_manager_android');

  static void registerWith() =>
      NfcManagerPlatform.instance = NfcManagerAndroidPlatform();

  @override
  Future<bool> isHceSupported() async =>
      (await methodChannel.invokeMethod<bool>('isHceSupported'))!;

  @override
  Future<bool> isNfcSupported() async =>
      (await methodChannel.invokeMethod<bool>('isNfcSupported'))!;
}
