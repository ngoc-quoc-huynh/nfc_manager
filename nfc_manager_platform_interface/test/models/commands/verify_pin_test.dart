import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  test('creates command correctly.', () {
    final pin = Uint8List.fromList([0x00]);
    final command = VerifyPinCommand(pin);

    expect(command.cla, 0x00);
    expect(command.ins, 0x20);
    expect(command.p1, 0x00);
    expect(command.p2, 0x00);
    expect(command.lc, pin.length);
    expect(command.data, pin);
  });

  group('toUint8List', () {
    test('returns correctly.', () {
      final pin = Uint8List.fromList([0x00]);
      final command = VerifyPinCommand(pin);
      final expected =
          Uint8List.fromList([0x00, 0x20, 0x00, 0x00, pin.length, ...pin]);

      expect(
        command.toUint8List(),
        expected,
      );
    });
  });
}
