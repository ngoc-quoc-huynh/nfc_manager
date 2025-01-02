import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

abstract base class NfcManagerPlatform extends PlatformInterface {
  NfcManagerPlatform(this.platform)
      : methodChannel = MethodChannel('${_baseChannel}_$platform'),
        discoveryEventChannel =
            EventChannel('${_baseChannel}_$platform/discovery'),
        hostCardEmulationEventChannel =
            EventChannel('${_baseChannel}_$platform/host_card_emulation'),
        super(token: _token);

  final String platform;

  @protected
  final MethodChannel methodChannel;

  @protected
  final EventChannel discoveryEventChannel;

  @protected
  final EventChannel hostCardEmulationEventChannel;

  static const String _baseChannel = 'dev.huynh/nfc_manager';

  // ignore: no-object-declaration, see https://pub.dev/packages/plugin_platform_interface
  static final Object _token = Object();

  static NfcManagerPlatform _instance = MethodChannelNfcManager();

  static NfcManagerPlatform get instance => _instance;

  static set instance(NfcManagerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<bool> isHceSupported() =>
      throw UnimplementedError('isHceSupported() has not been implemented.');

  Future<bool> isNfcSupported() =>
      throw UnimplementedError('isNfcSupported() has not been implemented.');

  Future<bool> isNfcEnabled() =>
      throw UnimplementedError('isNfcEnabled() has not been implemented.');

  Stream<String> startDiscovery({int? timeout}) =>
      throw UnimplementedError('startDiscovery() has not been implemented.');

  Future<ApduResponse> sendCommand(Command command) =>
      throw UnimplementedError('sendCommand() has not been implemented.');

  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) =>
      throw UnimplementedError('startEmulation() has not been implemented.');

  @visibleForTesting
  @protected
  // ignore: avoid-dynamic, error can be any type.
  bool isPlatformException(dynamic error) => error is PlatformException;

  @visibleForTesting
  @protected
  Never onStreamError(Object error) => switch (error) {
        PlatformException() => throw NfcException.fromPlatformException(error),
        _ => throw NfcUnknownException(error.toString()),
      };
}
