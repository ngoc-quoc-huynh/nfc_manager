import 'dart:typed_data';

import 'package:nfc_manager_platform_interface/src/exceptions.dart';
import 'package:nfc_manager_platform_interface/src/models/apdu_command.dart';
import 'package:nfc_manager_platform_interface/src/models/apdu_response.dart';
import 'package:nfc_manager_platform_interface/src/models/host_card_emulation_status.dart';
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
  /// Test mocks can implement this interface with `MockPlatformInterfaceMixin`
  /// from the `plugin_platform_interface` package.
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
  /// Each identifier is the tag ID as two uppercase hexadecimal digits per
  /// byte, for example `04A1B2C3`; decode it with `hexToBytes`.
  ///
  /// Discovery starts when the stream is listened to. Cancel the subscription
  /// to stop discovery.
  ///
  /// [timeout] limits communication with a discovered tag, not the time spent
  /// waiting for a tag. When it is not null, it must be a positive whole
  /// number of milliseconds that fits in a signed 32-bit integer (at most
  /// 2147483647); implementations may throw [ArgumentError] otherwise. When it
  /// is null, the platform's default applies.
  ///
  /// The stream ends when the platform's host UI detaches. Listen again after
  /// the host UI is attached again to resume discovery.
  ///
  /// The stream reports [NfcNotSupportedException] if the device does not
  /// support NFC, [IsoDepNotSupportedException] when a tag lacks ISO-DEP,
  /// [TagConnectionException] when connecting to a tag fails, and
  /// [NfcUnknownException] for any other platform error.
  Stream<String> startDiscovery({Duration? timeout}) =>
      throw UnimplementedError('startDiscovery() has not been implemented.');

  /// Sends an APDU command to the connected tag and returns its response.
  ///
  /// The [ApduResponse] keeps the response data and both status bytes.
  /// Reports [InvalidApduCommandException] if the platform rejects the
  /// command, [TagConnectionException] if there is no connection, the tag is
  /// lost, or the tag returns fewer than 2 bytes, and [NfcUnknownException] for
  /// any other platform error.
  Future<ApduResponse> sendCommand(ApduCommand command) =>
      throw UnimplementedError('sendCommand() has not been implemented.');

  /// Emulates a card using [aid] and [pin] and returns a stream of HCE events.
  ///
  /// Emulation starts when the stream is listened to. Cancel the subscription
  /// to stop emulation. Use one active emulation subscription at a time.
  ///
  /// The platform may require [aid] to be registered with the operating system
  /// as well. A ready event confirms that the configuration is stored. It does
  /// not confirm that NFC is enabled or that the device routes commands to the
  /// service.
  /// Choose the byte encoding of [pin] before calling this method.
  ///
  /// The stream reports [NfcUnknownException] for a platform error or an event
  /// this version does not know.
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) => throw UnimplementedError('startEmulation() has not been implemented.');
}

final class _UnimplementedNfcManagerPlatform extends NfcManagerPlatform {}
