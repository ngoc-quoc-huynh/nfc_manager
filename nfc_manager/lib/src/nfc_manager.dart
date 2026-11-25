import 'dart:typed_data';

import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// Provides access to the registered NFC platform implementation.
///
/// Android is currently the only implementation, and platform-specific
/// behavior described here is Android's. On a platform without an
/// implementation, every method throws [UnimplementedError] synchronously.
///
/// Futures can complete with `MissingPluginException` if the native plugin is
/// missing, and [isHceSupported], [isNfcSupported] and [isNfcEnabled] with
/// `PlatformException` if the native call fails. Other errors are
/// [NfcException] subtypes; [NfcUnknownException.code] identifies one without
/// a dedicated subtype. Treat [NfcException.message] as untrusted diagnostic
/// text: use the type or code in app logic and your own user-facing text.
class NfcManager {
  /// Creates an [NfcManager] that uses the registered NFC platform
  /// implementation.
  const NfcManager();

  /// The largest timeout in milliseconds, which fits in a signed 32-bit
  /// integer.
  static const _maxTimeoutMilliseconds = 0x7FFFFFFF;

  static NfcManagerPlatform get _platform => NfcManagerPlatform.instance;

  /// Checks if the device supports Host Card Emulation (HCE).
  Future<bool> isHceSupported() => _platform.isHceSupported();

  /// Checks if the device supports NFC.
  Future<bool> isNfcSupported() => _platform.isNfcSupported();

  /// Checks if NFC is enabled on the device. Returns false without an NFC
  /// adapter.
  Future<bool> isNfcEnabled() => _platform.isNfcEnabled();

  /// Discovers ISO-DEP tags and returns a stream of their identifiers.
  ///
  /// Each identifier is the tag ID as two uppercase hexadecimal digits per
  /// byte, for example `04A1B2C3`. Decode it with [HexString.hexToBytes].
  ///
  /// Discovery starts when the stream is listened to and stops when the
  /// subscription is cancelled. Only one discovery can run: before listening
  /// to a new stream, let the previous one finish or await its cancellation.
  /// Detaching the activity ends the stream; start again after reattachment.
  ///
  /// [timeout] limits each APDU exchange, not tag discovery or connection. A
  /// null [timeout] uses the platform default. Throws [ArgumentError]
  /// synchronously unless it is a positive whole number of milliseconds up to
  /// 2147483647.
  ///
  /// The stream reports an error and closes if discovery cannot start:
  /// [NfcNotSupportedException] without an NFC adapter, or
  /// [NfcUnknownException] with code `NFC_ERROR` without an attached activity
  /// or when reader mode fails. Per-tag errors keep discovery running:
  /// [IsoDepNotSupportedException] for a tag without ISO-DEP,
  /// [TagConnectionException] for a failed connection, and
  /// [NfcUnknownException] for other reader errors.
  Stream<String> startDiscovery({Duration? timeout}) {
    _checkTimeout(timeout);
    return _platform.startDiscovery(timeout: timeout);
  }

  /// Sends an APDU command to the connected tag and returns its response.
  ///
  /// The response holds the card's status bytes, including failure statuses;
  /// these are not thrown. The future completes with an error for:
  ///
  /// * [InvalidApduCommandException] if the command is longer than the tag's
  ///   maximum transceive length. It is not sent and the tag stays connected.
  /// * [TagConnectionException] if no tag is connected, the exchange fails,
  ///   or the response is shorter than 2 bytes.
  /// * [NfcUnknownException] for other native errors.
  Future<ApduResponse> sendCommand(ApduCommand command) =>
      _platform.sendCommand(command);

  /// Emulates a card using [aid] and [pin] and returns a stream of HCE events.
  ///
  /// Emulation starts when the stream is listened to, and
  /// [HostCardEmulationStatus.ready] means readers can now select [aid].
  /// Cancel the subscription to stop emulation. Only one emulation runs per
  /// app process: finish or cancel the current subscription before listening
  /// again. Listening from another Flutter engine replaces the emulation, and
  /// the replaced stream receives no further events.
  ///
  /// Declare [aid] in the HCE service configuration. Emulation does not check
  /// device support or NFC state, so `ready` is emitted even when NFC is off;
  /// check [isHceSupported] and [isNfcEnabled] first.
  ///
  /// Encode the [pin] text first, for example with `utf8.encode`. The reader
  /// must select [aid] before VERIFY; otherwise VERIFY is answered with
  /// `69 85` and does not count as a wrong PIN. Each listen allows 3 wrong
  /// PINs in a row; a correct PIN resets the count, and reconnecting or
  /// reselecting does not. The third ends the stream with
  /// [NfcUnknownException] code `PIN_BLOCKED`; offer the user a restart.
  ///
  /// Throws [RangeError] synchronously if [aid] is not 5–16 bytes or [pin] is
  /// not 1–255 bytes.
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) {
    checkAidLength(aid);
    checkPinLength(pin);

    return _platform.startEmulation(aid: aid, pin: pin);
  }

  static void _checkTimeout(Duration? timeout) {
    if (timeout != null &&
        (timeout <= .zero ||
            timeout.inMicroseconds % Duration.microsecondsPerMillisecond != 0 ||
            timeout.inMilliseconds > _maxTimeoutMilliseconds)) {
      throw ArgumentError.value(
        timeout,
        'timeout',
        'Must be a positive whole number of milliseconds up to '
            '$_maxTimeoutMilliseconds',
      );
    }
  }
}
