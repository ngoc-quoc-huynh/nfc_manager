import 'dart:typed_data';

import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

class NfcManager {
  factory NfcManager() => instance;

  const NfcManager._();

  static const NfcManager instance = NfcManager._();

  static final _platform = NfcManagerPlatform.instance;

  Future<bool> isHceSupported() => _platform.isHceSupported();

  Future<bool> isNfcSupported() => _platform.isNfcSupported();

  Future<bool> isNfcEnabled() => _platform.isNfcEnabled();

  Stream<String> startDiscovery({int? timeout}) =>
      _platform.startDiscovery(timeout: timeout);

  Future<ApduResponse> sendCommand(Command command) =>
      _platform.sendCommand(command);

  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) =>
      _platform.startEmulation(aid: aid, pin: pin);
}
