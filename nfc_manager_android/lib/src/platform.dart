import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:nfc_manager_android/src/messages.g.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// The Android implementation of [NfcManagerPlatform].
///
/// Android native errors without a dedicated [NfcException] subtype are
/// reported as [NfcUnknownException] with their native code and message.
///
/// * `INVALID_ARGUMENTS`: discovery or HCE received missing, mistyped, or
///   out-of-range arguments.
/// * `channel-error`: the Android plugin is not registered for this Flutter
///   engine. Feature checks throw it as a [PlatformException].
/// * `NFC_ERROR`: the NFC reader failed unexpectedly or discovery started
///   without an attached Android activity.
/// * `INVALID_EVENT_PAYLOAD`: discovery or HCE received a non-string event.
/// * `UNKNOWN_HCE_STATUS`: the HCE stream received an event name this
///   version does not know; the message holds that name.
/// * `PIN_BLOCKED`: the reader sent 3 wrong PINs in a row. The HCE stream
///   reports this error and ends, and further commands are answered with
///   `69 82` until emulation starts again.
///
/// An [InvalidApduCommandException] also means the command exceeds
/// `IsoDep.getMaxTransceiveLength`.
///
/// Discovery needs an attached activity. Detaching it ends the stream and
/// releases the tag. Streams from separate `startDiscovery` calls share one
/// native channel and can interrupt one another.
///
/// Flutter reports event-channel activation and cancellation failures through
/// [FlutterError].
final class NfcManagerAndroidPlatform extends NfcManagerPlatform {
  /// Constructs an [NfcManagerAndroidPlatform].
  NfcManagerAndroidPlatform();

  final _hostApi = NfcManagerHostApi();

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
  Future<bool> isHceSupported() => _hostApi.isHceSupported();

  @override
  Future<bool> isNfcSupported() => _hostApi.isNfcSupported();

  @override
  Future<bool> isNfcEnabled() => _hostApi.isNfcEnabled();

  @override
  Stream<String> startDiscovery({Duration? timeout}) => discoveryEventChannel
      .receiveBroadcastStream({'timeout': timeout?.inMilliseconds})
      .map(_decodeStringEvent)
      .handleError(_onStreamError, test: (e) => e is PlatformException);

  @override
  Future<ApduResponse> sendCommand(ApduCommand command) async {
    try {
      final response = await _hostApi.sendCommand(command.toUint8List());

      if (response.length < 2) {
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
      .handleError(_onStreamError, test: (e) => e is PlatformException);

  static String _decodeStringEvent(Object? event) => switch (event) {
    final String value => value,
    _ => throw const NfcUnknownException(code: 'INVALID_EVENT_PAYLOAD'),
  };

  static Never _onStreamError(Object error, StackTrace stackTrace) =>
      Error.throwWithStackTrace(
        _toNfcException(error as PlatformException),
        stackTrace,
      );

  /// Decodes a native error into its [NfcException].
  static NfcException _toNfcException(PlatformException exception) =>
      switch (exception.code) {
        'INVALID_APDU_COMMAND' => const InvalidApduCommandException(),
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
        'READY' => .ready,
        'INVALID_COMMAND' => .invalidCommand,
        'INVALID_LC_LENGTH' => .invalidLcLength,
        'AID_SELECTED' => .aidSelected,
        'INVALID_AID' => .invalidAid,
        'PIN_VERIFIED' => .pinVerified,
        'WRONG_PIN' => .wrongPin,
        'FUNCTION_NOT_SUPPORTED' => .functionNotSupported,
        'TAG_DISCONNECTED' => .tagDisconnected,
        _ => throw NfcUnknownException(
          code: 'UNKNOWN_HCE_STATUS',
          message: status,
        ),
      };
}
