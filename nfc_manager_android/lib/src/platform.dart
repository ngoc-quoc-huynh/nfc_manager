import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// The Android implementation of [NfcManagerPlatform].
final class NfcManagerAndroidPlatform extends NfcManagerPlatform {
  /// Constructs an [NfcManagerAndroidPlatform].
  NfcManagerAndroidPlatform();

  /// The Android request channel.
  @visibleForTesting
  final MethodChannel methodChannel = const MethodChannel(
    'dev.huynh/nfc_manager_android',
  );

  /// The Android discovery event channel.
  @visibleForTesting
  final EventChannel discoveryEventChannel = const EventChannel(
    'dev.huynh/nfc_manager_android/discovery',
  );

  /// The Android HCE event channel.
  @visibleForTesting
  final EventChannel hostCardEmulationEventChannel = const EventChannel(
    'dev.huynh/nfc_manager_android/host_card_emulation',
  );

  /// Registers the Android NFC implementation.
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
  Stream<String> startDiscovery({Duration? timeout}) => discoveryEventChannel
      .receiveBroadcastStream({'timeout': timeout?.inMilliseconds})
      .cast<String>()
      .handleError(_onStreamError, test: _isPlatformException);

  @override
  Future<ApduResponse> sendCommand(Command command) async {
    try {
      final response = await methodChannel.invokeListMethod<int>(
        'sendCommand',
        {'command': command.toUint8List()},
      );

      if (response == null) {
        throw const TagConnectionException();
      }

      return ApduResponse.fromUint8List(Uint8List.fromList(response));
    } on PlatformException catch (e, stackTrace) {
      Error.throwWithStackTrace(
        NfcException.fromPlatformException(e),
        stackTrace,
      );
    }
  }

  @override
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) => hostCardEmulationEventChannel
      .receiveBroadcastStream({'aid': aid, 'pin': pin})
      .cast<String>()
      .map(HostCardEmulationStatus.fromString)
      .handleError(_onStreamError, test: _isPlatformException);

  bool _isPlatformException(Object? error) => error is PlatformException;

  Never _onStreamError(Object error, StackTrace stackTrace) =>
      Error.throwWithStackTrace(
        NfcException.fromPlatformException(error as PlatformException),
        stackTrace,
      );
}
