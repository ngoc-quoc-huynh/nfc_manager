import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  test('creates command correctly.', () {
    final aid = Uint8List.fromList([0x00]);
    final command = SelectAidCommand(aid);

    expect(command.cla, 0x00);
    expect(command.ins, 0xA4);
    expect(command.p1, 0x04);
    expect(command.p2, 0x00);
    expect(command.lc, aid.length);
    expect(command.data, aid);
  });

  group('toUint8List', () {
    test('returns correctly.', () {
      final aid = Uint8List.fromList([0x00]);
      final command = SelectAidCommand(aid);
      final expected = Uint8List.fromList([
        0x00,
        0xA4,
        0x04,
        0x00,
        aid.length,
        ...aid,
      ]);

      expect(command.toUint8List(), expected);
    });
  });
}
