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
      .map(_decodeStringEvent)
      .handleError(_onStreamError, test: _isPlatformException);

  @override
  Future<ApduResponse> sendCommand(ApduCommand command) async {
    try {
      final response = await methodChannel.invokeMethod<Uint8List>(
        'sendCommand',
        {'command': command.toUint8List()},
      );

      // A response without both status bytes comes from the tag, so report it
      // as a failed exchange rather than a decoder FormatException.
      if (response == null || response.length < 2) {
        throw const TagConnectionException();
      }

      return ApduResponse.fromUint8List(response);
    } on PlatformException catch (e, stackTrace) {
      Error.throwWithStackTrace(_toNfcException(e), stackTrace);
    }
  }

  @override
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) => hostCardEmulationEventChannel
      .receiveBroadcastStream({'aid': aid, 'pin': pin})
      .map((event) => _toHostCardEmulationStatus(_decodeStringEvent(event)))
      .handleError(_onStreamError, test: _isPlatformException);

  static String _decodeStringEvent(Object? event) => switch (event) {
    final String value => value,
    _ => throw const NfcUnknownException(code: 'INVALID_EVENT_PAYLOAD'),
  };

  bool _isPlatformException(Object? error) => error is PlatformException;

  Never _onStreamError(Object error, StackTrace stackTrace) =>
      Error.throwWithStackTrace(
        _toNfcException(error as PlatformException),
        stackTrace,
      );

  /// Decodes a native error into its [NfcException].
  static NfcException _toNfcException(PlatformException exception) =>
      switch (exception.code) {
        'INVALID_APDU_COMMAND' => const InvalidApduCommandException(),
        'INVALID_LC_DATA_LENGTH' => const InvalidLcDataLengthException(),
        'ISO_DEP_NOT_SUPPORTED' => const IsoDepNotSupportedException(),
        'NFC_NOT_SUPPORTED' => const NfcNotSupportedException(),
        'TAG_CONNECTION_FAILED' => const TagConnectionException(),
        _ => NfcUnknownException(
          code: exception.code,
          message: exception.message,
        ),
      };

  /// Decodes a native HCE event name.
  ///
  /// Throws [NfcUnknownException] for an unrecognized name, so the stream
  /// reports it as an error and continues.
  static HostCardEmulationStatus _toHostCardEmulationStatus(String status) =>
      switch (status) {
        'READY' => HostCardEmulationStatus.ready,
        'INVALID_COMMAND' => HostCardEmulationStatus.invalidCommand,
        'INVALID_LC_LENGTH' => HostCardEmulationStatus.invalidLcLength,
        'AID_SELECTED' => HostCardEmulationStatus.aidSelected,
        'INVALID_AID' => HostCardEmulationStatus.invalidAid,
        'PIN_VERIFIED' => HostCardEmulationStatus.pinVerified,
        'WRONG_PIN' => HostCardEmulationStatus.wrongPin,
        'FUNCTION_NOT_SUPPORTED' =>
          HostCardEmulationStatus.functionNotSupported,
        'TAG_DISCONNECTED' => HostCardEmulationStatus.tagDisconnected,
        _ => throw NfcUnknownException(
          code: 'UNKNOWN_HCE_STATUS',
          message: status,
        ),
      };
}
