// Regenerate the outputs with `make pigeon` in the package directory.

// Pigeon 30.0.4 cannot read dot shorthands in its configuration annotation.
// ignore_for_file: prefer-dot-shorthands

import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/src/messages.g.dart',
    kotlinOut:
        'android/src/main/kotlin/dev/huynh/nfc_manager_android/Messages.g.kt',
    kotlinOptions: KotlinOptions(package: 'dev.huynh.nfc_manager_android'),
    dartPackageName: 'nfc_manager_android',
  ),
)
/// The requests Dart sends to the Android plugin.
///
/// Discovery and HCE stay on their own event channels because they need
/// arguments when the stream starts.
@HostApi()
abstract class NfcManagerHostApi {
  bool isHceSupported();

  bool isNfcSupported();

  bool isNfcEnabled();

  /// Sends an APDU command to the connected tag and returns its response.
  @asyncCallback
  Uint8List sendCommand(Uint8List command);
}
