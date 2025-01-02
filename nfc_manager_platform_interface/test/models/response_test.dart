import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  group('fromUint8List', () {
    test('returns correctly.', () {
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x6A, 0x81])),
        ApduResponse.functionNotSupported,
      );
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x69, 0x82])),
        ApduResponse.hceNotReader,
      );
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x6A, 0x82])),
        ApduResponse.invalidAid,
      );
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x6A, 0x00])),
        ApduResponse.nullCommand,
      );
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x90, 0x00])),
        ApduResponse.ok,
      );
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x67, 0x00])),
        ApduResponse.wrongLength,
      );
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x6C, 0x00])),
        ApduResponse.wrongLength,
      );
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x63, 0x00])),
        ApduResponse.wrongPin,
      );
    });

    test('returns unknown for invalid bytes', () {
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x00, 0x00])),
        ApduResponse.unknown,
      );
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x01, 0x02])),
        ApduResponse.unknown,
      );
      expect(
        ApduResponse.fromUint8List(Uint8List.fromList([0x03, 0x04])),
        ApduResponse.unknown,
      );
    });
  });
}
