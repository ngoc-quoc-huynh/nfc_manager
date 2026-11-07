import 'dart:typed_data';

import 'package:nfc_manager_platform_interface/src/exceptions.dart';
import 'package:nfc_manager_platform_interface/src/models/command.dart';
import 'package:nfc_manager_platform_interface/src/models/host_card_emulation_status.dart';
import 'package:nfc_manager_platform_interface/src/models/response.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

/// The interface for platform-specific NFC implementations.
///
/// Platform implementations must extend this class and set [instance] when
/// they register. Methods that are not overridden throw [UnimplementedError].
abstract class NfcManagerPlatform extends PlatformInterface {
  /// Constructs a [NfcManagerPlatform].
  NfcManagerPlatform() : super(token: _token);

  // Used to verify that registered implementations extend this class.
  static final Object _token = Object();

  static NfcManagerPlatform _instance = _UnimplementedNfcManagerPlatform();

  /// The NFC platform implementation to use.
  ///
  /// Until an implementation registers, all methods throw [UnimplementedError].
  static NfcManagerPlatform get instance => _instance;

  /// Registers an implementation that extends [NfcManagerPlatform].
  ///
  /// Test mocks can implement this interface with `MockPlatformInterfaceMixin`.
  static set instance(NfcManagerPlatform instance) {
    PlatformInterface.verify(instance, _token);
    _instance = instance;
  }

  /// Whether the device supports Host Card Emulation (HCE).
  Future<bool> isHceSupported() =>
      throw UnimplementedError('isHceSupported() has not been implemented.');

  /// Whether the device supports NFC.
  Future<bool> isNfcSupported() =>
      throw UnimplementedError('isNfcSupported() has not been implemented.');

  /// Whether NFC is currently enabled.
  Future<bool> isNfcEnabled() =>
      throw UnimplementedError('isNfcEnabled() has not been implemented.');

  /// Discovers ISO-DEP tags and returns a stream of their identifiers.
  ///
  /// Discovery starts when the stream is listened to. Cancel the subscription
  /// to stop discovery. [timeout] limits communication with a discovered tag,
  /// not the time spent waiting for a tag.
  ///
  /// The stream reports [IsoDepNotSupportedException] when a tag lacks ISO-DEP,
  /// and [TagConnectionException] when connecting to a tag fails.
  Stream<String> startDiscovery({Duration? timeout}) =>
      throw UnimplementedError('startDiscovery() has not been implemented.');

  /// Sends an APDU command to the connected tag and returns its response.
  ///
  /// See [ApduResponse] for the supported response format. Reports
  /// [TagConnectionException] if there is no connection or the tag is lost.
  Future<ApduResponse> sendCommand(Command command) =>
      throw UnimplementedError('sendCommand() has not been implemented.');

  /// Emulates a card using [aid] and [pin] and returns a stream of HCE events.
  ///
  /// Emulation starts when the stream is listened to. Cancel the subscription
  /// to stop emulation. Use one active emulation subscription at a time.
  ///
  /// On Android, [aid] must also be registered for the HCE service. A ready
  /// event confirms that the configuration is stored. It does not confirm that
  /// NFC is enabled or that the device routes commands to the service.
  /// Choose the byte encoding of [pin] before calling this method.
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) => throw UnimplementedError('startEmulation() has not been implemented.');
}

final class _UnimplementedNfcManagerPlatform extends NfcManagerPlatform {}
