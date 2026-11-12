import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

Matcher _throwsLengthError(int length, int start, int end, String name) =>
    throwsA(
      isA<RangeError>()
          .having((error) => error.invalidValue, 'invalidValue', length)
          .having((error) => error.start, 'start', start)
          .having((error) => error.end, 'end', end)
          .having((error) => error.name, 'name', name),
    );

void main() {
  group('checkAidLength', () {
    test('rejects AIDs of 4 and 17 bytes.', () {
      for (final length in [4, 17]) {
        expect(
          () => checkAidLength(Uint8List(length)),
          _throwsLengthError(length, 5, 16, 'aid.length'),
          reason: 'AID length: $length',
        );
      }
    });

    test('accepts AIDs of 5 and 16 bytes.', () {
      for (final length in [5, 16]) {
        expect(
          () => checkAidLength(Uint8List(length)),
          returnsNormally,
          reason: 'AID length: $length',
        );
      }
    });
  });

  group('checkPinLength', () {
    test('rejects PINs of 0 and 256 bytes.', () {
      for (final length in [0, 256]) {
        expect(
          () => checkPinLength(Uint8List(length)),
          _throwsLengthError(length, 1, 255, 'pin.length'),
          reason: 'PIN length: $length',
        );
      }
    });

    test('accepts PINs of 1 and 255 bytes.', () {
      for (final length in [1, 255]) {
        expect(
          () => checkPinLength(Uint8List(length)),
          returnsNormally,
          reason: 'PIN length: $length',
        );
      }
    });
  });
}
