import 'dart:typed_data';

import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// Provides access to the registered NFC platform implementation.
///
/// Every method documents the errors it reports. A platform error without a
/// dedicated [NfcException] subtype is reported as [NfcUnknownException]; its
/// `code` identifies the error. Treat the `message` of any [NfcException] as
/// untrusted diagnostic text: do not parse it, show it to users, or rely on
/// it being stable.
class NfcManager {
  /// Creates an [NfcManager] that uses the registered NFC platform
  /// implementation.
  const NfcManager();

  /// The largest timeout in milliseconds, which fits in a signed 32-bit
  /// integer.
  static const _maxTimeoutMilliseconds = 0x7FFFFFFF;

  static NfcManagerPlatform get _platform => NfcManagerPlatform.instance;

  /// Checks if the device supports Host Card Emulation (HCE).
  ///
  /// Throws [UnimplementedError] if no platform implementation is registered
  /// or it does not support this method.
  Future<bool> isHceSupported() => _platform.isHceSupported();

  /// Checks if the device supports NFC.
  ///
  /// Throws [UnimplementedError] if no platform implementation is registered
  /// or it does not support this method.
  Future<bool> isNfcSupported() => _platform.isNfcSupported();

  /// Checks if NFC is enabled on the device.
  ///
  /// Throws [UnimplementedError] if no platform implementation is registered
  /// or it does not support this method.
  Future<bool> isNfcEnabled() => _platform.isNfcEnabled();

  /// Discovers ISO-DEP tags and returns a stream of their identifiers.
  ///
  /// Each identifier is the tag ID as two uppercase hexadecimal digits per
  /// byte, for example `04A1B2C3`. Decode it with `hexToBytes`.
  ///
  /// Discovery starts when the stream is listened to. Cancel the subscription
  /// to stop discovery. Use one active discovery subscription at a time; a new
  /// one stops the previous one, which then receives no further events.
  /// [timeout] limits communication with a discovered tag, not the time spent
  /// waiting for a tag.
  ///
  /// The stream ends when the platform's host UI detaches. To resume, call
  /// [startDiscovery] again after the host UI reattaches and listen to the new
  /// stream.
  ///
  /// Throws [UnimplementedError] if no platform implementation is registered
  /// or it does not support this method.
  ///
  /// Throws [ArgumentError] when this method is called if [timeout] is not a
  /// positive whole number of milliseconds up to 2147483647.
  ///
  /// The stream reports [NfcNotSupportedException] if the device does not
  /// support NFC, [IsoDepNotSupportedException] when a tag lacks ISO-DEP,
  /// [TagConnectionException] when connecting to a tag fails, and
  /// [NfcUnknownException] for any other platform error.
  Stream<String> startDiscovery({Duration? timeout}) {
    if (timeout != null) {
      _checkTimeout(timeout);
    }

    return _platform.startDiscovery(timeout: timeout);
  }

  /// Sends an APDU command to the connected tag and returns its response.
  ///
  /// Throws [UnimplementedError] if no platform implementation is registered
  /// or it does not support this method.
  ///
  /// Throws [InvalidApduCommandException] if the platform rejects [command].
  ///
  /// Throws [TagConnectionException] if there is no connection, the tag is
  /// lost, or the tag returns fewer than 2 bytes.
  ///
  /// Throws [NfcUnknownException] for any other platform error.
  Future<ApduResponse> sendCommand(ApduCommand command) =>
      _platform.sendCommand(command);

  /// Emulates a card using [aid] and [pin] and returns a stream of HCE events.
  ///
  /// Emulation starts when the stream is listened to. Cancel the subscription
  /// to stop emulation. Use one active emulation subscription at a time. The
  /// platform may need [aid] to be registered as well; on Android, declare it
  /// in the HCE service configuration. A ready event confirms that the
  /// configuration is stored, not that NFC is enabled or that commands are
  /// routed to your app.
  ///
  /// Encode the [pin] text before calling this method, for example with
  /// `utf8.encode`.
  ///
  /// Each call starts a new emulation session with its own PIN retry budget.
  /// On Android, 3 wrong PINs in a row end the stream with an
  /// [NfcUnknownException] whose code is `PIN_BLOCKED`. The count carries
  /// over when the reader disconnects or selects the [aid] again, and only a
  /// correct PIN or a new call resets it. Do not restart emulation
  /// automatically after `PIN_BLOCKED`; let the user decide.
  ///
  /// Throws [UnimplementedError] if no platform implementation is registered
  /// or it does not support this method.
  ///
  /// Throws [RangeError] when this method is called if [aid] does not contain
  /// 5–16 bytes or [pin] does not contain 1–255 bytes.
  ///
  /// The stream reports [NfcUnknownException] for a platform error or an
  /// event this version does not know.
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) {
    checkAidLength(aid);
    checkPinLength(pin);

    return _platform.startEmulation(aid: aid, pin: pin);
  }

  static void _checkTimeout(Duration timeout) {
    if (timeout <= .zero ||
        timeout.inMicroseconds % Duration.microsecondsPerMillisecond != 0 ||
        timeout.inMilliseconds > _maxTimeoutMilliseconds) {
      throw ArgumentError.value(
        timeout,
        'timeout',
        'Must be a positive whole number of milliseconds up to '
            '$_maxTimeoutMilliseconds',
      );
    }
  }
}
