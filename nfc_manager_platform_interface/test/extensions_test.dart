import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  group('toHexString', () {
    test('pads each byte to two hexadecimal digits.', () {
      expect(String.fromCharCodes([0x00, 0x01, 0x0F]).toHexString(), '00010F');
    });

    test('encodes UTF-8 bytes and round-trips non-ASCII text.', () {
      for (final (text, hex, bytes) in [
        (String.fromCharCode(0xE9), 'C3A9', [0xC3, 0xA9]),
        (String.fromCharCode(0x1F600), 'F09F9880', [0xF0, 0x9F, 0x98, 0x80]),
      ]) {
        expect(text.toHexString(), hex);
        expect(text.toUint8List(), bytes);
        expect(text.toHexString().toUint8List(isHex: true), bytes);
      }
    });

    test(
      'replaces unpaired surrogates with the UTF-8 replacement character.',
      () {
        for (final codeUnit in [0xD800, 0xDC00]) {
          expect(String.fromCharCode(codeUnit).toHexString(), 'EFBFBD');
        }
      },
    );

    test('encodes ASCII text as uppercase hexadecimal.', () {
      expect('Hello'.toHexString(), '48656C6C6F');
      expect('World'.toHexString(), '576F726C64');
      expect('!'.toHexString(), '21');
      expect('Hello world!'.toHexString(), '48656C6C6F20776F726C6421');
    });
  });

  group('toUint8List', () {
    test('preserves empty input and accepts lowercase hex.', () {
      expect(''.toHexString(), '');
      expect(''.toUint8List(), isEmpty);
      expect(''.toUint8List(isHex: true), isEmpty);
      expect('aBcd'.toUint8List(isHex: true), [0xAB, 0xCD]);
    });

    test('rejects signed, prefixed, and non-hex input.', () {
      for (final value in ['-1', '+1', 'GG', '0x12', ' 1', '1 ', '12\n']) {
        expect(
          () => value.toUint8List(isHex: true),
          throwsFormatException,
          reason: 'input: $value',
        );
      }
    });
    test('encodes ASCII text as UTF-8 bytes.', () {
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

    test('decodes hexadecimal byte pairs.', () {
      expect('00'.toUint8List(isHex: true), Uint8List.fromList([0]));
      expect(
        '0123456789ABCDEF'.toUint8List(isHex: true),
        Uint8List.fromList([1, 35, 69, 103, 137, 171, 205, 239]),
      );
    });

    test('decodes all byte values in order in both hex cases.', () {
      const hex =
          '000102030405060708090A0B0C0D0E0F'
          '101112131415161718191A1B1C1D1E1F'
          '202122232425262728292A2B2C2D2E2F'
          '303132333435363738393A3B3C3D3E3F'
          '404142434445464748494A4B4C4D4E4F'
          '505152535455565758595A5B5C5D5E5F'
          '606162636465666768696A6B6C6D6E6F'
          '707172737475767778797A7B7C7D7E7F'
          '808182838485868788898A8B8C8D8E8F'
          '909192939495969798999A9B9C9D9E9F'
          'A0A1A2A3A4A5A6A7A8A9AAABACADAEAF'
          'B0B1B2B3B4B5B6B7B8B9BABBBCBDBEBF'
          'C0C1C2C3C4C5C6C7C8C9CACBCCCDCECF'
          'D0D1D2D3D4D5D6D7D8D9DADBDCDDDEDF'
          'E0E1E2E3E4E5E6E7E8E9EAEBECEDEEEF'
          'F0F1F2F3F4F5F6F7F8F9FAFBFCFDFEFF';
      final expected = List.generate(256, (i) => i);

      expect(hex.toUint8List(isHex: true), expected);
      expect(hex.toLowerCase().toUint8List(isHex: true), expected);
    });

    test(
      'replaces unpaired surrogates with the UTF-8 replacement character.',
      () {
        for (final codeUnit in [0xD800, 0xDC00]) {
          expect(String.fromCharCode(codeUnit).toUint8List(), [
            0xEF,
            0xBF,
            0xBD,
          ]);
        }
      },
    );

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
