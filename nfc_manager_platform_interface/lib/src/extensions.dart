import 'dart:convert';
import 'dart:typed_data';

/// Provides extension methods for the [String] class to enhance string
/// manipulation capabilities in NFC context.
extension StringExtensions on String {
  /// Converts the string to a hexadecimal string.
  String toHexString() =>
      runes
          .map((rune) => rune.toRadixString(16).padLeft(2, '0'))
          .join()
          .toUpperCase();

  /// Converts the string to a [Uint8List].
  ///
  /// If [isHex] is true, the string is assumed to be a hexadecimal string and
  /// converted to a [Uint8List] of bytes.
  ///
  /// If [isHex] is false, the string is assumed to be a UTF-8 encoded string
  /// and converted to a [Uint8List] of bytes.
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
