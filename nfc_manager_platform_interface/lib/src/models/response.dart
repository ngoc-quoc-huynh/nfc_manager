import 'dart:typed_data';

enum ApduResponse {
  functionNotSupported,
  hceNotReader,
  invalidAid,
  nullCommand,
  ok,
  unknown,
  wrongLcLength,
  wrongLength,
  wrongPin;

  factory ApduResponse.fromUint8List(Uint8List bytes) => switch (bytes) {
        [0x6A, 0x81] => functionNotSupported,
        [0x69, 0x82] => hceNotReader,
        [0x6A, 0x82] => invalidAid,
        [0x6A, 0x00] => nullCommand,
        [0x90, 0x00] => ok,
        [0x67, 0x00] => wrongLength,
        [0x6C, 0x00] => wrongLength,
        [0x63, 0x00] => wrongPin,
        _ => unknown,
      };
}
