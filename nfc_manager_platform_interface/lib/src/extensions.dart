import 'dart:typed_data';

/// Decodes hexadecimal strings, such as AIDs, into bytes.
///
/// Encode text, such as a PIN, with `utf8.encode` from `dart:convert`.
extension HexString on String {
  /// Decodes hexadecimal byte pairs into bytes.
  ///
  /// Accepts the digits 0–9, a–f, and A–F. Empty input returns an empty
  /// buffer. Throws [FormatException] for an odd length or any other
  /// character, including signs, prefixes, whitespace, and non-ASCII digits.
  /// The exception never contains the input, which may be a secret; it only
  /// reports the offset of the first invalid character.
  Uint8List hexToBytes() {
    if (length.isOdd) {
      throw const FormatException('Hex string must have an even length.');
    }

    final bytes = Uint8List(length ~/ 2);
    for (var i = 0; i < bytes.length; i++) {
      bytes[i] = _nibble(i * 2) << 4 | _nibble(i * 2 + 1);
    }
    return bytes;
  }

  int _nibble(int index) {
    final unit = codeUnitAt(index);
    return switch (unit) {
      >= 0x30 && <= 0x39 => unit - 0x30, // 0-9
      >= 0x41 && <= 0x46 => unit - 0x41 + 10, // A-F
      >= 0x61 && <= 0x66 => unit - 0x61 + 10, // a-f
      _ => throw FormatException(
        'Hex string must contain only hexadecimal digits.',
        null,
        index,
      ),
    };
  }
}

/// Encodes bytes, such as tag IDs, as hexadecimal strings.
extension HexBytes on Uint8List {
  /// Returns two uppercase hexadecimal digits per byte.
  ///
  /// Uppercase matches the tag IDs reported by Android.
  String toHex() {
    final buffer = StringBuffer();
    for (final byte in this) {
      buffer.write(byte.toRadixString(16).padLeft(2, '0').toUpperCase());
    }
    return buffer.toString();
  }
}
