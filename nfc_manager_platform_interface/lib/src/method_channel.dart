import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// Implementation of [NfcManagerPlatform] using method channel.
final class MethodChannelNfcManager extends NfcManagerPlatform {
  /// Constructs a new [MethodChannelNfcManager].
  MethodChannelNfcManager() : super('platform');

  @override
  Future<bool> isHceSupported() async =>
      (await methodChannel.invokeMethod<bool>('isHceSupported'))!;

  @override
  Future<bool> isNfcSupported() async =>
      (await methodChannel.invokeMethod<bool>('isNfcSupported'))!;

  @override
  Future<bool> isNfcEnabled() async =>
      (await methodChannel.invokeMethod<bool>('isNfcEnabled'))!;

  @override
  Stream<String> startDiscovery({Duration? timeout}) => discoveryEventChannel
      .receiveBroadcastStream({'timeout': timeout?.inMilliseconds})
      .cast<String>()
      .handleError(onStreamError, test: isPlatformException);

  @override
  Future<ApduResponse> sendCommand(Command command) async {
    try {
      final response = await methodChannel.invokeListMethod<int>(
        'sendCommand',
        {'command': command.toUint8List()},
      );

      return ApduResponse.fromUint8List(Uint8List.fromList(response!));
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
  }) => hostCardEmulationEventChannel
      .receiveBroadcastStream({'aid': aid, 'pin': pin})
      .cast<String>()
      .map(HostCardEmulationStatus.fromString)
      .handleError(onStreamError, test: isPlatformException);
}
