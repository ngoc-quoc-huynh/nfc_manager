import 'dart:typed_data';

import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// Provides a high-level interface for interacting with the device's NFC
/// hardware. Implemented as a singleton to centralize NFC interactions within
/// the application.
///
/// This class facilitates checking NFC support and status, initiating NFC data
/// discovery, transmitting commands, and managing host card emulation. Utilize
/// this class to effortlessly incorporate NFC functionalities into your
/// application.
class NfcManager {
  /// Constructs an instance of [NfcManager].
  factory NfcManager() => _instance;

  const NfcManager._();

  static const NfcManager _instance = NfcManager._();

  static final _platform = NfcManagerPlatform.instance;

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

  /// Starts NFC discovery.
  ///
  /// If a [timeout] is specified and exceeded, a [TagConnectionException] is
  /// thrown.
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  ///
  /// Throws [IsoDepNotSupportedException] if the device does not support
  /// ISO-DEP.
  ///
  /// Throws [TagConnectionException] when the application fails to establish a
  /// connection with the NFC tag or the tag is lost during the discovery
  /// process.
  Stream<String> startDiscovery({Duration? timeout}) =>
      _platform.startDiscovery(timeout: timeout);

  /// Sends an APDU command to an NFC tag.
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  ///
  /// Throws [TagConnectionException] when the application fails to establish a
  /// connection with the NFC tag or the tag was lost.
  Future<ApduResponse> sendCommand(Command command) =>
      _platform.sendCommand(command);

  /// Starts NFC HCE with the provided AID and PIN.
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) => _platform.startEmulation(aid: aid, pin: pin);
}
