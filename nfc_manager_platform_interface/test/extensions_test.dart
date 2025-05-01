import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  group('toHexString', () {
    test('returns correctly', () {
      expect('Hello'.toHexString(), '48656C6C6F');
      expect('World'.toHexString(), '576F726C64');
      expect('!'.toHexString(), '21');
      expect('Hello world!'.toHexString(), '48656C6C6F20776F726C6421');
    });
  });

  group('toUint8List', () {
    test('returns correctly when isHex is false.', () {
      expect(
        'Hello'.toUint8List(),
        Uint8List.fromList([72, 101, 108, 108, 111]),
      );
      expect(
        'World'.toUint8List(),
        Uint8List.fromList([87, 111, 114, 108, 100]),
      );
      expect('!'.toUint8List(), Uint8List.fromList([33]));
      expect(
        'Hello world!'.toUint8List(),
        Uint8List.fromList([
          72,
          101,
          108,
          108,
          111,
          32,
          119,
          111,
          114,
          108,
          100,
          33,
        ]),
      );
    });

    test('returns correctly when isHex is true.', () {
      expect('00'.toUint8List(isHex: true), Uint8List.fromList([0]));
      expect(
        '0123456789ABCDEF'.toUint8List(isHex: true),
        Uint8List.fromList([1, 35, 69, 103, 137, 171, 205, 239]),
      );
    });

    test(
      'throws FormatException when isHex is true but length is odd.',
      () => expect(
        () => '0'.toUint8List(isHex: true),
        throwsA(
          isA<FormatException>().having(
            (e) => e.message,
            'message',
            'Hex string must have an even length.',
          ),
        ),
      ),
    );
  });
}
