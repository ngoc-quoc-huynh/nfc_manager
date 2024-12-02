import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/src/platform.dart';

final class MethodChannelNfcManager extends NfcManagerPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('dev.huynh/nfc_manager');

  @override
  Future<bool> isNfcSupported() async =>
      (await methodChannel.invokeMethod<bool>('isNfcSupported'))!;
}
