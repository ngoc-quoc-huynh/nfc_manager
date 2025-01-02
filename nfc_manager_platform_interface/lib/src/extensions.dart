import 'dart:convert';
import 'dart:typed_data';

extension StringExtensions on String {
  String toHexString() => runes
      .map((rune) => rune.toRadixString(16).padLeft(2, '0'))
      .join()
      .toUpperCase();

  Uint8List toUint8List({bool isHex = false}) => switch (isHex) {
        false => Uint8List.fromList(utf8.encode(this)),
        true when length.isOdd =>
          throw const FormatException('Hex string must have an even length.'),
        true => Uint8List.fromList(
            List.generate(
              length ~/ 2,
              (i) => int.parse(
                // ignore: avoid-substring, currently only UTF-8 supported.
                substring(i * 2, i * 2 + 2),
                radix: 16,
              ),
            ),
          ),
      };
}
