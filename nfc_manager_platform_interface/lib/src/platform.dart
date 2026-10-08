import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

/// Mixin for creating mock implementations of [NfcManagerPlatform].
///
/// This mixin is designed for testing purposes, allowing developers to create
/// mock implementations of the [NfcManagerPlatform]. It can be used to
/// simulate NFC platform behavior in a controlled test environment without
/// interacting with actual NFC hardware.
@visibleForTesting
base mixin MockNfcManagerPlatformMixin implements NfcManagerPlatform {}

/// Abstract base class for all NFC manager platforms.
///
/// This class provides a unified interface and essential functionality for all
/// NFC platform implementations. It standardizes the API across various
/// platform-specific implementations, enabling seamless integration of NFC
/// capabilities in a platform-independent way.
abstract base class NfcManagerPlatform extends PlatformInterface {
  /// Constructs a new [NfcManagerPlatform].
  NfcManagerPlatform(this.platform)
    : methodChannel = MethodChannel('${_baseChannel}_$platform'),
      discoveryEventChannel = EventChannel(
        '${_baseChannel}_$platform/discovery',
      ),
      hostCardEmulationEventChannel = EventChannel(
        '${_baseChannel}_$platform/host_card_emulation',
      ),
      super(token: _token);

  /// A string identifier for the specific platform implementation.
  final String platform;

  /// A [MethodChannel] used for sending messages from the Dart side to the
  /// native side of the platform.
  @visibleForTesting
  @protected
  final MethodChannel methodChannel;

  /// An [EventChannel] used to receive NFC discovery events from the native
  /// platform layer.
  @visibleForTesting
  @protected
  final EventChannel discoveryEventChannel;

  /// An [EventChannel] used to receive host card emulation events from the
  /// native platform layer.
  @visibleForTesting
  @protected
  final EventChannel hostCardEmulationEventChannel;

  /// Defines the base channel name used for communication between the Dart and
  /// native sides of the platform.
  static const String _baseChannel = 'dev.huynh/nfc_manager';

  /// A unique token used to verify the authenticity of the platform instance.
  // ignore: no-object-declaration, see https://pub.dev/packages/plugin_platform_interface
  static final Object _token = Object();

  /// A singleton instance representing the specific platform implementation of
  /// [NfcManagerPlatform].
  static NfcManagerPlatform _instance = MethodChannelNfcManager();

  /// Provides the current instance of the platform-specific implementation of
  /// [NfcManagerPlatform].
  static NfcManagerPlatform get instance => _instance;

  /// Assigns a new default instance for the [NfcManagerPlatform] to be used
  /// across the platform.
  static set instance(NfcManagerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// Checks if the device supports Host Card Emulation (HCE).
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  Future<bool> isHceSupported() =>
      throw UnimplementedError('isHceSupported() has not been implemented.');

  /// Checks if the device supports NFC.
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  Future<bool> isNfcSupported() =>
      throw UnimplementedError('isNfcSupported() has not been implemented.');

  /// Checks if NFC is enabled.
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  Future<bool> isNfcEnabled() =>
      throw UnimplementedError('isNfcEnabled() has not been implemented.');

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
      throw UnimplementedError('startDiscovery() has not been implemented.');

  /// Sends an APDU command to an NFC tag.
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  ///
  /// Throws [TagConnectionException] when the application fails to establish a
  /// connection with the NFC tag or the tag was lost.
  Future<ApduResponse> sendCommand(Command command) =>
      throw UnimplementedError('sendCommand() has not been implemented.');

  /// Starts NFC HCE with the provided AID and PIN.
  ///
  /// Throws [UnimplementedError] if the method is not implemented for the
  /// platform.
  Stream<HostCardEmulationStatus> startEmulation({
    required Uint8List aid,
    required Uint8List pin,
  }) => throw UnimplementedError('startEmulation() has not been implemented.');

  /// Checks if the provided error object is an instance of [PlatformException].
  @visibleForTesting
  @protected
  // ignore: avoid-dynamic, error can be any type.
  bool isPlatformException(dynamic error) => error is PlatformException;

  /// Manages error handling for stream operations.
  @visibleForTesting
  @protected
  Never onStreamError(Object error) => switch (error) {
    PlatformException() => throw NfcException.fromPlatformException(error),
    _ => throw NfcUnknownException(error.toString()),
  };
}
