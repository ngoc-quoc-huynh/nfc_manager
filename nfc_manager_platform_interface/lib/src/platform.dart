import 'package:nfc_manager_platform_interface/src/method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

abstract base class NfcManagerPlatform extends PlatformInterface {
  NfcManagerPlatform() : super(token: _token);

  // ignore: no-object-declaration, see https://pub.dev/packages/plugin_platform_interface
  static final Object _token = Object();

  static NfcManagerPlatform _instance = MethodChannelNfcManager();

  static NfcManagerPlatform get instance => _instance;

  static set instance(NfcManagerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> foo() {
    throw UnimplementedError('foo() has not been implemented.');
  }
}
