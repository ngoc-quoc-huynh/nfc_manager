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
  /// Start discovery when the stream is listened to. Emit an identifier
  /// after connecting to the tag. Encode each byte of the tag ID as two
  /// uppercase hexadecimal digits, for example `04A1B2C3`. Decode the ID
  /// with `hexToBytes`.
  ///
  /// Maintain one active tag connection for [sendCommand]. A new successful
  /// connection replaces the previous one. Release the connection when
  /// discovery ends or the subscription is cancelled. Use one discovery
  /// subscription at a time. To restart, wait for completion or cancel the
  /// subscription, then call [startDiscovery] and listen to the new stream.
  ///
  /// [timeout] applies to each APDU exchange after the tag connects. It must
  /// be a positive whole number of milliseconds up to 2147483647. A null
  /// value uses the platform default.
  ///
  /// Report [NfcNotSupportedException] and close the stream if the device
  /// lacks NFC. Report [IsoDepNotSupportedException] for a tag without ISO-DEP
  /// or [TagConnectionException] for a failed connection. Keep discovering
  /// after these per-tag errors. Use [NfcUnknownException] for discovery
  /// errors without a dedicated exception type. Close the stream if discovery
  /// cannot start.
  Stream<String> startDiscovery({Duration? timeout}) =>
      throw UnimplementedError('startDiscovery() has not been implemented.');

  /// Sends an APDU command to the connected tag and returns its response.
  ///
  /// The [ApduResponse] keeps the response data and both status bytes.
  /// Reports [InvalidApduCommandException] if the platform rejects the
  /// command. Card status bytes, including failure statuses, are returned in
  /// [ApduResponse].
  ///
  /// [TagConnectionException] reports a missing connection, a lost tag, an
  /// I/O failure during the exchange, or a response with fewer than 2 bytes.
  /// [NfcUnknownException] reports native NFC errors without a dedicated
  /// exception type.
  Future<ApduResponse> sendCommand(ApduCommand command) =>
      throw UnimplementedError('sendCommand() has not been implemented.');

  /// Emulates a card using [aid] and [pin] and returns a stream of HCE events.
  ///
  /// The [aid] must contain 5 to 16 bytes and the [pin] must contain 1 to 255
  /// bytes.
  ///
  /// Emulation starts when the stream is listened to. Cancel the subscription
  /// to stop emulation. Use one active emulation subscription at a time.
  ///
  /// On Android, [aid] must also be registered for the HCE service. A ready
  /// event confirms that the configuration is stored. It does not confirm that
  /// NFC is enabled or that the device routes commands to the service.
  /// Choose the byte encoding of [pin] before calling this method.
  ///
  /// The stream reports [NfcUnknownException] for native HCE errors without a
  /// dedicated exception type and for unrecognized or malformed HCE events.
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) => throw UnimplementedError('startEmulation() has not been implemented.');
}

final class _UnimplementedNfcManagerPlatform extends NfcManagerPlatform {}
