import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

typedef _FixedMessageCase = (
  String name,
  NfcException Function() create,
  String toStringText,
);

void main() {
  group('NfcException', () {
    test(
      'different exception types are unequal even with the same message.',
      () {
        const exception = NfcNotSupportedException();
        final unknown = NfcUnknownException(
          code: 'NFC_NOT_SUPPORTED',
          message: exception.message,
        );

        expect(exception, isNot(unknown));
        expect(unknown, isNot(exception));
      },
    );
  });

  // Each entry creates a new instance, so equality is not identity.
  for (final (name, create, text) in <_FixedMessageCase>[
    (
      'InvalidApduCommandException',
      InvalidApduCommandException.new,
      'InvalidApduCommandException: The platform rejected the APDU command.',
    ),
    (
      'IsoDepNotSupportedException',
      IsoDepNotSupportedException.new,
      'IsoDepNotSupportedException: The NFC tag does not support ISO-DEP.',
    ),
    (
      'NfcNotSupportedException',
      NfcNotSupportedException.new,
      'NfcNotSupportedException: This device does not support NFC.',
    ),
    (
      'TagConnectionException',
      TagConnectionException.new,
      'TagConnectionException: Failed to establish a connection with the NFC '
          'tag or the tag was lost.',
    ),
  ]) {
    group(name, () {
      test(
        'toString shows the type and message.',
        () => expect(create().toString(), text),
      );

      test('equal instances are equal and have equal hash codes.', () {
        final exception = create();
        final equalException = create();

        expect(exception, equalException);
        expect(exception.hashCode, equalException.hashCode);
      });
    });
  }

  group('NfcUnknownException', () {
    test(
      'equal codes and messages produce equal exceptions and hash codes.',
      () {
        for (final message in <String?>[null, 'message']) {
          final exception = NfcUnknownException(code: 'code', message: message);
          final equalException = NfcUnknownException(
            code: 'code',
            message: message,
          );

          expect(exception, equalException, reason: 'message: $message');
          expect(
            exception.hashCode,
            equalException.hashCode,
            reason: 'message: $message',
          );
        }
      },
    );

    test('different messages produce unequal exceptions.', () {
      expect(
        const NfcUnknownException(code: 'code', message: 'first'),
        isNot(const NfcUnknownException(code: 'code', message: 'second')),
      );
      expect(
        const NfcUnknownException(code: 'code'),
        isNot(const NfcUnknownException(code: 'code', message: '')),
      );
    });

    test('different codes produce unequal exceptions.', () {
      const first = NfcUnknownException(code: 'FIRST', message: 'message');
      const second = NfcUnknownException(code: 'SECOND', message: 'message');

      expect(first, isNot(second));
    });

    test('exposes the native code and message.', () {
      const exception = NfcUnknownException(code: 'CODE', message: 'message');

      expect(exception.code, 'CODE');
      expect(exception.message, 'message');
      expect(const NfcUnknownException(code: 'CODE').message, isNull);
    });

    test(
      'toString shows the type, code and message.',
      () => expect(
        const NfcUnknownException(code: 'CODE', message: 'message').toString(),
        'NfcUnknownException(CODE): message',
      ),
    );

    test(
      'toString keeps an explicitly empty message.',
      () => expect(
        const NfcUnknownException(code: 'CODE', message: '').toString(),
        'NfcUnknownException(CODE): ',
      ),
    );

    test(
      'toString omits a missing message.',
      () => expect(
        const NfcUnknownException(code: 'CODE').toString(),
        'NfcUnknownException(CODE)',
      ),
    );
  });
}
