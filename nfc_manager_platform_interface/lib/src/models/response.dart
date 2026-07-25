import 'dart:typed_data';

/// Represents the possible responses from an APDU command.
enum ApduResponse {
  /// Indicates that the function is not supported.
  functionNotSupported,

  /// Indicates that the HCE is not ready to read.
  hceNotReady,

  /// Indicates that the AID is invalid.
  invalidAid,

  /// Indicates that the command is null.
  nullCommand,

  /// Indicates that the command is OK.
  ok,

  /// Indicates that the response is unknown.
  unknown,

  /// Indicates that the length of the command data is invalid.
  wrongLcLength,

  /// Indicates that the length of the command data is invalid.
  wrongLength,

  /// Indicates that the PIN is incorrect.
  wrongPin;

  /// Constructs an [ApduResponse] from a [Uint8List].
  factory ApduResponse.fromUint8List(Uint8List bytes) => switch (bytes) {
        [0x6A, 0x81] => functionNotSupported,
        [0x69, 0x82] => hceNotReady,
        [0x6A, 0x82] => invalidAid,
        [0x6A, 0x00] => nullCommand,
        [0x90, 0x00] => ok,
        [0x67, 0x00] => wrongLength,
        [0x6C, 0x00] => wrongLength,
        [0x63, 0x00] => wrongPin,
        _ => unknown,
      };
}
