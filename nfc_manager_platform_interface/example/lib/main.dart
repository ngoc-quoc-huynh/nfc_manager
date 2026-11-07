import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

final class NfcManagerExamplePlatform() extends NfcManagerPlatform {
  final _methodChannel = const MethodChannel('dev.huynh/nfc_manager_example');
  final _discoveryEventChannel = const EventChannel(
    'dev.huynh/nfc_manager_example/discovery',
  );
  final _hostCardEmulationEventChannel = const EventChannel(
    'dev.huynh/nfc_manager_example/host_card_emulation',
  );

  static void registerWith() =>
      NfcManagerPlatform.instance = NfcManagerExamplePlatform();

  @override
  Future<bool> isHceSupported() async =>
      (await _methodChannel.invokeMethod<bool>('isHceSupported'))!;

  @override
  Future<bool> isNfcSupported() async =>
      (await _methodChannel.invokeMethod<bool>('isNfcSupported'))!;

  @override
  Future<bool> isNfcEnabled() async =>
      (await _methodChannel.invokeMethod<bool>('isNfcEnabled'))!;

  @override
  Stream<String> startDiscovery({Duration? timeout}) => _discoveryEventChannel
      .receiveBroadcastStream({'timeout': timeout?.inMilliseconds})
      .cast<String>()
      .handleError(_onStreamError, test: _isPlatformException);

  @override
  Future<ApduResponse> sendCommand(Command command) async {
    try {
      final response = await _methodChannel.invokeListMethod<int>(
        'sendCommand',
        {'command': command.toUint8List()},
      );

      return .fromUint8List(.fromList(response!));
    } on PlatformException catch (e, stackTrace) {
      Error.throwWithStackTrace(
        NfcException.fromPlatformException(e),
        stackTrace,
      );
    }
  }

  @override
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) => _hostCardEmulationEventChannel
      .receiveBroadcastStream({'aid': aid, 'pin': pin})
      .cast<String>()
      .map(HostCardEmulationStatus.fromString)
      .handleError(_onStreamError, test: _isPlatformException);

  bool _isPlatformException(Object? error) => error is PlatformException;

  Never _onStreamError(Object error) =>
      throw NfcException.fromPlatformException(error as PlatformException);
}
