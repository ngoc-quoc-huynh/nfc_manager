import 'dart:typed_data';

/// Classifies the two-byte status responses supported by this plugin.
///
/// Payload-bearing responses and other status words are classified as
/// [unknown].
/// This enum does not preserve response payloads or status-word parameters.
enum ApduResponse {
  /// Indicates that the function is not supported.
  functionNotSupported,

  /// HCE is not configured in this plugin's protocol.
  hceNotReady,

  /// Indicates that the AID is invalid.
  invalidAid,

  /// Indicates that the command is null.
  nullCommand,

  /// Indicates that the command is OK.
  ok,

  /// Indicates that the response is unknown.
  unknown,

  /// The command data length is invalid (67 00).
  wrongLcLength,

  /// The expected response length needs correction (6C 00).
  wrongLength,

  /// Indicates that the PIN is incorrect.
  wrongPin;

  /// Classifies an exact two-byte response, or returns [unknown].
  factory ApduResponse.fromUint8List(Uint8List bytes) => switch (bytes) {
    [0x6A, 0x81] => functionNotSupported,
    [0x69, 0x82] => hceNotReady,
    [0x6A, 0x82] => invalidAid,
    [0x6A, 0x00] => nullCommand,
    [0x90, 0x00] => ok,
    [0x67, 0x00] => wrongLcLength,
    [0x6C, 0x00] => wrongLength,
    [0x63, 0x00] => wrongPin,
    _ => unknown,
  };
}
