import 'dart:typed_data';

import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// Provides access to the registered NFC platform implementation.
///
/// On Android, native NFC errors mapped by command or stream operations use
/// an [NfcException] subtype. [NfcUnknownException.code] identifies an error
/// without a dedicated subtype. Treat [NfcException.message] as untrusted
/// diagnostic text. Use the exception type or code in app logic and your own
/// text in user-facing messages.
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
  /// Throws [UnimplementedError] synchronously if there is no registered
  /// implementation for this method.
  ///
  /// On Android, a failed native call can complete the future with
  /// `PlatformException` or, if the native handler is missing,
  /// `MissingPluginException`.
  Future<bool> isHceSupported() => _platform.isHceSupported();

  /// Checks if the device supports NFC.
  ///
  /// Throws [UnimplementedError] synchronously if there is no registered
  /// implementation for this method.
  ///
  /// On Android, a failed native call can complete the future with
  /// `PlatformException` or, if the native handler is missing,
  /// `MissingPluginException`.
  Future<bool> isNfcSupported() => _platform.isNfcSupported();

  /// Checks if NFC is enabled on the device.
  ///
  /// On Android, the result is false when the device has no NFC adapter.
  ///
  /// Throws [UnimplementedError] synchronously if there is no registered
  /// implementation for this method.
  ///
  /// On Android, a failed native call can complete the future with
  /// `PlatformException` or, if the native handler is missing,
  /// `MissingPluginException`.
  Future<bool> isNfcEnabled() => _platform.isNfcEnabled();

  /// Discovers ISO-DEP tags and returns a stream of their identifiers.
  ///
  /// Each identifier is the tag ID as two uppercase hexadecimal digits per
  /// byte, for example `04A1B2C3`. Decode it with [HexString.hexToBytes].
  ///
  /// Discovery starts when the stream is listened to. Keep only one active
  /// discovery subscription across all calls to [startDiscovery]. To stop
  /// discovery early, cancel the subscription. To restart, wait for the stream
  /// to finish or await cancellation, then call [startDiscovery] and listen
  /// to the new stream.
  ///
  /// On Android, [timeout] applies to each APDU exchange after the tag
  /// connects. Tag discovery and connection are outside this timeout.
  ///
  /// On Android, detaching the host UI (the activity) ends the stream. To
  /// resume, call [startDiscovery] after reattachment and listen to the new
  /// stream.
  ///
  /// Throws [UnimplementedError] synchronously if there is no registered
  /// implementation for this method.
  ///
  /// Throws [ArgumentError] synchronously if a non-null [timeout] is not a
  /// positive whole number of milliseconds up to 2147483647. A null [timeout]
  /// uses the platform default.
  ///
  /// On Android, the stream reports [NfcUnknownException] with code `NFC_ERROR`
  /// and closes if there is no attached activity or Android throws while
  /// enabling reader mode.
  /// With an attached activity but no NFC adapter, it reports
  /// [NfcNotSupportedException] and closes.
  ///
  /// Per-tag errors allow discovery to continue. These include
  /// [IsoDepNotSupportedException] when a tag lacks ISO-DEP and
  /// [TagConnectionException] when the connection fails. Other reader errors
  /// and invalid event payloads can also reach the listener as
  /// [NfcUnknownException]. Flutter reports event-channel activation and
  /// cancellation failures through `FlutterError`.
  Stream<String> startDiscovery({Duration? timeout}) {
    if (timeout != null) {
      _checkTimeout(timeout);
    }

    return _platform.startDiscovery(timeout: timeout);
  }

  /// Sends an APDU command to the connected tag and returns its response.
  ///
  /// Throws [UnimplementedError] synchronously if there is no registered
  /// implementation for this method.
  ///
  /// On Android, the returned future can complete with these errors.
  ///
  /// * [InvalidApduCommandException] if the platform rejects [command], for
  ///   example when it exceeds the known transceive limit. Android rejects
  ///   such a command before transmission and keeps the tag connected.
  /// * [TagConnectionException] if there is no active connection, an I/O
  ///   failure interrupts the exchange, or the response is missing either
  ///   status byte.
  /// * [NfcUnknownException] for other native error codes.
  /// * `MissingPluginException` if the native handler is missing.
  ///
  /// Card status bytes, including failure statuses, are returned in
  /// [ApduResponse].
  Future<ApduResponse> sendCommand(ApduCommand command) =>
      _platform.sendCommand(command);

  /// Emulates a card using [aid] and [pin] and returns a stream of HCE events.
  ///
  /// Listening to a new stream starts emulation with a fresh PIN retry budget.
  /// Cancel the subscription to stop emulation. Finish or cancel the existing
  /// subscription before listening to a new stream. The platform may need [aid]
  /// to be registered as well; on Android, declare it in the HCE service
  /// configuration.
  /// [HostCardEmulationStatus.ready] confirms that the configuration is stored.
  /// Use [isHceSupported] and [isNfcEnabled] to check device capabilities and
  /// NFC state separately. Routing depends on the Android service setup.
  ///
  /// Encode the [pin] text before calling this method, for example with
  /// `utf8.encode`.
  ///
  /// On Android, 3 wrong PINs in a row end the stream with an
  /// [NfcUnknownException] whose code is `PIN_BLOCKED`. The count
  /// carries over when the reader disconnects or selects the [aid] again. A
  /// correct PIN resets it. Offer the user a restart after `PIN_BLOCKED`.
  ///
  /// Throws [UnimplementedError] synchronously if there is no registered
  /// implementation for this method.
  ///
  /// Throws [RangeError] synchronously if [aid] is outside 5–16 bytes or
  /// [pin] is outside 1–255 bytes. Both ranges are inclusive.
  ///
  /// On Android, native errors without a dedicated subtype arrive as
  /// [NfcUnknownException] stream errors. An invalid event payload or an
  /// unrecognized HCE status also reports [NfcUnknownException] and allows the
  /// stream to continue. Flutter reports event-channel activation and
  /// cancellation failures through `FlutterError`.
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
