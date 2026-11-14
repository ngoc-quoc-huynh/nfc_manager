import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

ApduResponse _decode(List<int> bytes) =>
    ApduResponse.fromUint8List(Uint8List.fromList(bytes));

void main() {
  group('fromUint8List', () {
    test('splits a status-only response into empty data and SW1-SW2.', () {
      final response = _decode([0x6A, 0x82]);

      expect(response.data, isEmpty);
      expect(response.sw1, 0x6A);
      expect(response.sw2, 0x82);
      expect(response.statusWord, 0x6A82);
    });

    test('keeps the payload that precedes the status word.', () {
      final response = _decode([0x6F, 0x03, 0x84, 0x01, 0xF0, 0x90, 0x00]);

      expect(response.data, [0x6F, 0x03, 0x84, 0x01, 0xF0]);
      expect(response.sw1, 0x90);
      expect(response.sw2, 0x00);
      expect(response.status, ApduStatus.success);
      expect(response.isSuccess, isTrue);
    });

    test('exposes data as an unmodifiable view.', () {
      final response = _decode([0x01, 0x90, 0x00]);

      expect(() => response.data[0] = 0xFF, throwsUnsupportedError);
    });

    test('keeps its value when the source buffer changes.', () {
      final bytes = Uint8List.fromList([0x01, 0x90, 0x00]);
      final response = ApduResponse.fromUint8List(bytes);
      final expected = _decode([0x01, 0x90, 0x00]);
      final hash = response.hashCode;

      bytes[0] = 0xFF;

      expect(response.data, [0x01]);
      expect(response, expected);
      expect(response.hashCode, hash);
    });

    test('throws FormatException for fewer than two bytes.', () {
      for (final bytes in <List<int>>[
        [],
        [0x90],
      ]) {
        expect(() => _decode(bytes), throwsFormatException);
      }
    });
  });

  group('status', () {
    test('maps each supported status word to its status.', () {
      for (final (bytes, status) in [
        ([0x90, 0x00], ApduStatus.success),
        ([0x63, 0x00], ApduStatus.verificationFailed),
        ([0x67, 0x00], ApduStatus.wrongLength),
        ([0x69, 0x82], ApduStatus.securityStatusNotSatisfied),
        ([0x69, 0x83], ApduStatus.authenticationMethodBlocked),
        ([0x6A, 0x00], ApduStatus.wrongParameters),
        ([0x6A, 0x81], ApduStatus.functionNotSupported),
        ([0x6A, 0x82], ApduStatus.fileOrApplicationNotFound),
        ([0x6C, 0x00], ApduStatus.wrongLeField),
      ]) {
        expect(_decode(bytes).status, status, reason: '$bytes');
      }
    });

    test('classifies the trailing status word of a payload response.', () {
      expect(_decode([0xAB, 0x63, 0x00]).status, ApduStatus.verificationFailed);
    });

    test('returns unknown for unsupported status words.', () {
      for (final bytes in <List<int>>[
        [0x00, 0x00],
        [0x01, 0x02],
        [0x6D, 0x00],
      ]) {
        expect(_decode(bytes).status, ApduStatus.unknown, reason: '$bytes');
      }
    });

    test('classifies the 63 CX and 6C XX families and keeps SW2.', () {
      for (final (bytes, status) in [
        ([0x63, 0xC0], ApduStatus.verificationFailed),
        ([0x63, 0xC2], ApduStatus.verificationFailed),
        ([0x63, 0xCF], ApduStatus.verificationFailed),
        ([0x6C, 0x10], ApduStatus.wrongLeField),
        ([0x6C, 0xFF], ApduStatus.wrongLeField),
      ]) {
        final response = _decode(bytes);

        expect(response.status, status, reason: '$bytes');
        expect(response.sw2, bytes[1], reason: '$bytes');
      }
    });

    test('returns unknown for near misses and keeps SW2.', () {
      for (final (bytes, sw2) in [
        ([0x63, 0x01], 0x01),
        ([0x63, 0xD0], 0xD0),
        ([0x90, 0x01], 0x01),
        ([0x61, 0x10], 0x10),
      ]) {
        final response = _decode(bytes);

        expect(response.status, ApduStatus.unknown, reason: '$bytes');
        expect(response.sw2, sw2, reason: '$bytes');
        expect(response.isSuccess, isFalse, reason: '$bytes');
      }
    });
  });

  group('isSuccess', () {
    test('is true only for 90 00.', () {
      expect(_decode([0x90, 0x00]).isSuccess, isTrue);
      expect(_decode([0x6A, 0x82]).isSuccess, isFalse);
    });
  });

  group('equality', () {
    test('compares data and status bytes.', () {
      expect(_decode([0x01, 0x90, 0x00]), _decode([0x01, 0x90, 0x00]));
      expect(
        _decode([0x01, 0x90, 0x00]).hashCode,
        _decode([0x01, 0x90, 0x00]).hashCode,
      );
      expect(_decode([0x01, 0x90, 0x00]), isNot(_decode([0x02, 0x90, 0x00])));
      expect(_decode([0x90, 0x00]), isNot(_decode([0x90, 0x01])));
    });
  });

  test('toString hides the data contents.', () {
    expect(
      _decode([0x6F, 0x01, 0x90, 0x00]).toString(),
      'ApduResponse(data: 2 bytes, statusWord: 9000, status: success)',
    );
  });
}
