import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

base class NfcManagerAndroidPlatform extends NfcManagerPlatform {
  factory NfcManagerAndroidPlatform() => _instance;

  @visibleForTesting
  factory NfcManagerAndroidPlatform.test() => NfcManagerAndroidPlatform._();

  NfcManagerAndroidPlatform._();

  static final _instance = NfcManagerAndroidPlatform._();

  @visibleForTesting
  static const methodChannel = MethodChannel('dev.huynh/nfc_manager_android');

  @visibleForTesting
  static const discoveryEventChannel =
      EventChannel('dev.huynh/nfc_manager_android/discovery');

  @visibleForTesting
  bool isDiscoveryStarted = false;

  final Stream<Tag> _discoveryStream = discoveryEventChannel
      .receiveBroadcastStream()
      .cast<Tag>()
      .asBroadcastStream();

  static void registerWith() =>
      NfcManagerPlatform.instance = NfcManagerAndroidPlatform();

  @override
  Future<bool> isHceSupported() async =>
      (await methodChannel.invokeMethod<bool>('isHceSupported'))!;

  @override
  Future<bool> isNfcSupported() async =>
      (await methodChannel.invokeMethod<bool>('isNfcSupported'))!;

  @override
  Future<bool> isNfcEnabled() async =>
      (await methodChannel.invokeMethod<bool>('isNfcEnabled'))!;

  @override
  Stream<Tag> startDiscovery() async* {
    if (!isDiscoveryStarted) {
      try {
        await methodChannel.invokeMethod<void>('startDiscovery');
      } on PlatformException catch (e) {
        throw NfcException.fromCode(
          code: e.code,
          message: e.message,
        );
      }
      isDiscoveryStarted = true;
    }

    yield* _discoveryStream;
  }

  @override
  Future<void> stopDiscovery() async {
    try {
      await methodChannel.invokeMethod<void>('stopDiscovery');
    } on PlatformException catch (e) {
      throw NfcException.fromCode(
        code: e.code,
        message: e.message,
      );
    }
    isDiscoveryStarted = false;
  }
}
