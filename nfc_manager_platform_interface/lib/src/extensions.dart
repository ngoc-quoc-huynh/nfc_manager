import 'dart:convert';
import 'dart:typed_data';

/// Converts text and hexadecimal strings to NFC byte payloads.
extension StringExtensions on String {
  static final _invalidHexCharacter = RegExp('[^0-9a-fA-F]');

  /// Encodes the text as UTF-8 and returns uppercase hexadecimal bytes.
  ///
  /// Unpaired UTF-16 surrogates are replaced with U+FFFD before encoding.
  String toHexString() => utf8
      .encode(this)
      .map((byte) => byte.toRadixString(16).padLeft(2, '0'))
      .join()
      .toUpperCase();

  /// Encodes text as UTF-8, or decodes hexadecimal byte pairs when [isHex].
  ///
  /// Throws [FormatException] for incomplete byte pairs or characters outside
  /// 0–9, a–f, and A–F. Empty input produces an empty buffer in either mode.
  /// When encoding text, unpaired UTF-16 surrogates are replaced with U+FFFD.
  Uint8List toUint8List({bool isHex = false}) {
    if (!isHex) {
      return utf8.encode(this);
    }

    if (length.isOdd) {
      throw const FormatException('Hex string must have an even length.');
    }

    if (_invalidHexCharacter.hasMatch(this)) {
      throw const FormatException(
        'Hex string must contain only hexadecimal digits.',
      );
    }

    final bytes = Uint8List(length ~/ 2);
    for (var i = 0; i < bytes.length; i++) {
      bytes[i] = int.parse(
        // ignore: avoid-substring, each validated pair encodes one byte.
        substring(i * 2, i * 2 + 2),
        radix: 16,
      );
    }
    return bytes;
  }
}
