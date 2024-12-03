import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/src/exceptions.dart';

void main() {
  group('NfcException', () {
    test('fromCode returns correctly.', () {
      expect(
        NfcException.fromCode(code: 'NFC_NOT_SUPPORTED', message: 'message'),
        const NfcNotSupportedException(),
      );
      expect(
        NfcException.fromCode(code: 'Unknown', message: 'message'),
        const NfcUnknownException('message'),
      );
    });

    group('NfcUnknownException', () {
      test(
        'returns hashcode correctly.',
        () => expect(
          const NfcUnknownException('').hashCode,
          1,
        ),
      );

      test(
        'toString returns correctly',
        () => expect(
          const NfcUnknownException('message').toString(),
          'NfcUnknownException: message.',
        ),
      );
    });

    group('NfcNotSupportedException', () {
      test(
        'returns hashcode correctly.',
        () => expect(
          const NfcNotSupportedException().hashCode,
          765580442,
        ),
      );

      test(
        'toString returns correctly',
        () => expect(
          const NfcNotSupportedException().toString(),
          'NfcNotSupportedException: This device does not support NFC.',
        ),
      );
    });
  });
}
