import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  group('toUint8List', () {
    test(
      'returns correctly when all parameters are provided.',
      () => expect(
        Command(
          cla: 0x00,
          ins: 0x00,
          p1: 0x00,
          p2: 0x00,
          lc: 0x01,
          data: Uint8List.fromList([0x00]),
          le: 0x00,
        ).toUint8List(),
        Uint8List.fromList([0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00]),
      ),
    );

    test(
      'returns correctly when lc, data and le are null.',
      () => expect(
        const Command(cla: 0x00, ins: 0x00, p1: 0x00, p2: 0x00).toUint8List(),
        Uint8List.fromList([0x00, 0x00, 0x00, 0x00]),
      ),
    );
  });

  group('equality', () {
    test('returns true when parameters are identical.', () {
      final command1 = Command(
        cla: 0x00,
        ins: 0x00,
        p1: 0x00,
        p2: 0x00,
        lc: 0x01,
        data: Uint8List.fromList([0x00]),
        le: 0x00,
      );
      final command2 = Command(
        cla: 0x00,
        ins: 0x00,
        p1: 0x00,
        p2: 0x00,
        lc: 0x01,
        data: Uint8List.fromList([0x00]),
        le: 0x00,
      );
      expect(command1, command2);
      expect(command1.hashCode, command2.hashCode);
    });

    test(
      'returns true when parameters are identical and lc, data and le are '
      'null.',
      () {
        const command1 = Command(cla: 0x00, ins: 0x00, p1: 0x00, p2: 0x00);
        const command2 = Command(cla: 0x00, ins: 0x00, p1: 0x00, p2: 0x00);
        expect(command1, command2);
        expect(command1.hashCode, command2.hashCode);
      },
    );

    test('returns false when parameters are different.', () {
      const command1 = Command(cla: 0x00, ins: 0x00, p1: 0x00, p2: 0x00);
      const command2 = Command(cla: 0x00, ins: 0x00, p1: 0x00, p2: 0x01);
      expect(command1, isNot(command2));
      expect(command1.hashCode, isNot(command2.hashCode));
    });
  });
}
