import 'dart:typed_data';

import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// Provides access to the registered NFC platform implementation.
class NfcManager {
  /// Returns the shared [NfcManager] instance.
  factory NfcManager() => _instance;

  const NfcManager._();

  static const NfcManager _instance = NfcManager._();

  static NfcManagerPlatform get _platform => NfcManagerPlatform.instance;

  /// Checks if the device supports Host Card Emulation (HCE).
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  Future<bool> isHceSupported() => _platform.isHceSupported();

  /// Checks if the device supports NFC.
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  Future<bool> isNfcSupported() => _platform.isNfcSupported();

  /// Checks if NFC is enabled on the device.
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  Future<bool> isNfcEnabled() => _platform.isNfcEnabled();

  /// Discovers ISO-DEP tags and returns a stream of their identifiers.
  ///
  /// Discovery starts when the stream is listened to. Cancel the subscription
  /// to stop discovery. Use one discovery stream at a time. Before starting
  /// another, let the current stream finish or cancel its subscription.
  /// [timeout] limits communication with a discovered tag,
  /// not the time spent waiting for a tag.
  ///
  /// On Android, the stream ends when the activity detaches. After it
  /// reattaches, call [startDiscovery] again and listen to the new stream.
  ///
  /// See [NfcManagerPlatform.startDiscovery] for stream errors.
  Stream<String> startDiscovery({Duration? timeout}) =>
      _platform.startDiscovery(timeout: timeout);

  /// Sends an APDU command to the connected tag and returns its response.
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  ///
  /// Throws [TagConnectionException] if there is no connection or the tag is
  /// lost.
  Future<ApduResponse> sendCommand(Command command) =>
      _platform.sendCommand(command);

  /// Emulates a card using [aid] and [pin] and returns a stream of HCE events.
  ///
  /// Emulation starts when the stream is listened to. Cancel the subscription
  /// to stop emulation. On Android, [aid] must also be registered for the HCE
  /// service. See [NfcManagerPlatform.startEmulation]
  /// for configuration and subscription requirements.
  ///
  /// Throws [RangeError] when this method is called if [aid] does not contain
  /// 5–16 bytes or [pin] does not contain 1–255 bytes.
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) {
    checkAidLength(aid);
    checkPinLength(pin);

    return _platform.startEmulation(aid: aid, pin: pin);
  }
}
