import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// The Android implementation of [NfcManagerPlatform].
///
/// Android native errors without a dedicated [NfcException] subtype are
/// reported as [NfcUnknownException] with their native code and message.
/// The Dart adapter also uses this exception for event decoding errors.
///
/// * `INVALID_ARGUMENTS`: the plugin received missing, mistyped, or
///   out-of-range arguments.
/// * `NFC_ERROR`: the NFC reader failed unexpectedly or discovery started
///   without an attached Android activity.
/// * `INVALID_EVENT_PAYLOAD`: discovery or HCE received a non-string event.
/// * `UNKNOWN_HCE_STATUS`: the HCE stream received an event name this
///   version does not know; the message holds that name.
/// * `PIN_BLOCKED`: the reader sent 3 wrong PINs in a row. The HCE stream
///   reports this error and ends, and further commands are answered with
///   `69 82` until emulation starts again.
///
/// An [InvalidApduCommandException] can mean the command is longer than
/// `IsoDep.getMaxTransceiveLength` allows; the tag stays connected.
///
/// Flutter reports event-channel activation and cancellation failures through
/// [FlutterError].
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

  /// The first listener starts reader mode. When the last listener cancels,
  /// the reader stops and releases the active tag. Detaching the Android
  /// activity ends the stream and releases the tag. To resume, call
  /// [startDiscovery] after reattachment and listen to the new stream.
  ///
  /// The public `NfcManager` throws [ArgumentError] synchronously for an
  /// invalid [timeout]. Direct calls convert the duration with
  /// [Duration.inMilliseconds], which truncates fractional milliseconds. If
  /// the forwarded value is outside 1–2147483647, the stream reports
  /// [NfcUnknownException] with code `INVALID_ARGUMENTS` and ends. A null
  /// value uses the Android default.
  ///
  /// If the activity is missing or enabling reader mode fails, the stream
  /// reports [NfcUnknownException] with code `NFC_ERROR` and ends. With an
  /// attached activity but no NFC adapter, the stream reports
  /// [NfcNotSupportedException] and ends. Discovery continues after individual
  /// tag errors.
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

  /// Invalid AID or PIN lengths produce a [NfcUnknownException] with code
  /// `INVALID_ARGUMENTS` after the stream is listened to. The stream then ends.
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
