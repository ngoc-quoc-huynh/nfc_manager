import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/src/platform.dart';

final class MethodChannelNfcManager extends NfcManagerPlatform {
  @visibleForTesting
  static const methodChannel = MethodChannel('dev.huynh/nfc_manager');

  @override
  Future<bool> isHceSupported() async =>
      (await methodChannel.invokeMethod<bool>('isHceSupported'))!;

  @override
  Future<bool> isNfcSupported() async =>
      (await methodChannel.invokeMethod<bool>('isNfcSupported'))!;

  @override
  Future<bool> isNfcEnabled() async =>
      (await methodChannel.invokeMethod<bool>('isNfcEnabled'))!;
}
