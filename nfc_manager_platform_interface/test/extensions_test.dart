import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  group('HexString.hexToBytes', () {
    test(
      'returns an empty buffer for empty input.',
      () => expect(''.hexToBytes(), isEmpty),
    );

    test('decodes every digit in either case.', () {
      final expected = [0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF];

      expect('0123456789ABCDEF'.hexToBytes(), expected);
      expect('0123456789abcdef'.hexToBytes(), expected);
      expect('aBcD'.hexToBytes(), [0xAB, 0xCD]);
    });

    test('decodes boundary byte values.', () {
      expect('00'.hexToBytes(), [0x00]);
      expect('7F80'.hexToBytes(), [0x7F, 0x80]);
      expect('FF'.hexToBytes(), [0xFF]);
    });

    test('rejects odd-length input without exposing it.', () {
      for (final value in ['0', '123', 'ABCDE', 'G']) {
        expect(
          value.hexToBytes,
          throwsA(
            allOf(
              _formatError(value, 'Hex string must have an even length.'),
              isA<FormatException>().having((e) => e.offset, 'offset', isNull),
            ),
          ),
          reason: 'input: $value',
        );
      }
    });

    test('rejects signed, prefixed, spaced, and non-hex ASCII input.', () {
      for (final (value, offset) in [
        ('-1', 0),
        ('+1', 0),
        ('GG', 0),
        ('g0', 0),
        ('0x12', 1),
        (' 1', 0),
        ('1 ', 1),
        ('1\n', 1),
        ('12\n3', 2),
        ('/0', 0),
        (':0', 0),
        ('@0', 0),
        ('`0', 0),
      ]) {
        expect(
          value.hexToBytes,
          throwsA(_invalidDigitError(value, offset)),
          reason: 'input: $value',
        );
      }
    });

    test('rejects non-ASCII input, including Unicode digits.', () {
      for (final codeUnits in [
        // Fullwidth digits one and two.
        [0xFF11, 0xFF12],
        // Arabic-Indic digits one and two.
        [0x0661, 0x0662],
        // Latin small e with acute accent, paired with a valid digit.
        [0x00E9, 0x30],
        // Unpaired surrogates.
        [0xD800, 0x30],
        [0xDC00, 0x30],
        // Code units whose low byte is a hex digit.
        [0x0130, 0x30],
        [0x0141, 0x30],
      ]) {
        final value = String.fromCharCodes(codeUnits);
        expect(
          value.hexToBytes,
          throwsA(_invalidDigitError(value, 0)),
          reason: 'code units: $codeUnits',
        );
      }
    });

    test('rejects a valid surrogate pair.', () {
      // U+1F600 is two UTF-16 code units, so the length check passes.
      final value = String.fromCharCode(0x1F600);

      expect(value, hasLength(2));
      expect(value.hexToBytes, throwsA(_invalidDigitError(value, 0)));
    });

    test('round-trips with HexBytes.toHex.', () {
      final bytes = Uint8List.fromList(.generate(256, (i) => i));

      expect(bytes.toHex().hexToBytes(), bytes);
    });
  });

  group('HexBytes.toHex', () {
    test(
      'returns an empty string for an empty buffer.',
      () => expect(Uint8List(0).toHex(), ''),
    );

    test(
      'pads each byte to two uppercase digits.',
      () => expect(
        Uint8List.fromList([0x00, 0x01, 0x0F, 0x10, 0xAB, 0xFF]).toHex(),
        '00010F10ABFF',
      ),
    );
  });
}

Matcher _formatError(String input, String message) => isA<FormatException>()
    .having((e) => e.message, 'message', message)
    .having((e) => e.message, 'message', isNot(contains(input)))
    .having((e) => e.source, 'source', isNull);

Matcher _invalidDigitError(String input, int offset) => allOf(
  _formatError(input, 'Hex string must contain only hexadecimal digits.'),
  isA<FormatException>().having((e) => e.offset, 'offset', offset),
);
