import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

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

  group('InvalidApduCommandException', () {
    test('exposes the exception message.', () {
      expect(
        const InvalidApduCommandException().message,
        'APDU command must be at least 4 bytes long.',
      );
    });

    test(
      'includes the exception type and message in toString.',
      () => expect(
        const InvalidApduCommandException().toString(),
        'InvalidApduCommandException: APDU command must be at least 4 bytes'
        ' long.',
      ),
    );

    test('equal exceptions have equal hash codes.', () {
      const exception = InvalidApduCommandException();
      // ignore: prefer_const_constructors, verify separate instances.
      final equalException = InvalidApduCommandException();

      expect(exception, equalException);
      expect(exception.hashCode, equalException.hashCode);
    });
  });

  group('InvalidLcDataLengthException', () {
    test('exposes the exception message.', () {
      expect(
        const InvalidLcDataLengthException().message,
        'The LC value does not match the length of the Data field in the APDU'
        ' command.',
      );
    });

    test(
      'includes the exception type and message in toString.',
      () => expect(
        const InvalidLcDataLengthException().toString(),
        'InvalidLcDataLengthException: The LC value does not match the length '
        'of the Data field in the APDU command.',
      ),
    );

    test('equal exceptions have equal hash codes.', () {
      const exception = InvalidLcDataLengthException();
      // ignore: prefer_const_constructors, verify separate instances.
      final equalException = InvalidLcDataLengthException();

      expect(exception, equalException);
      expect(exception.hashCode, equalException.hashCode);
    });
  });

  group('IsoDepNotSupportedException', () {
    test('exposes the exception message.', () {
      expect(
        const IsoDepNotSupportedException().message,
        'The NFC tag does not support ISO-DEP.',
      );
    });

    test(
      'includes the exception type and message in toString.',
      () => expect(
        const IsoDepNotSupportedException().toString(),
        'IsoDepNotSupportedException: The NFC tag does not support ISO-DEP.',
      ),
    );

    test('equal exceptions have equal hash codes.', () {
      const exception = IsoDepNotSupportedException();
      // ignore: prefer_const_constructors, verify separate instances.
      final equalException = IsoDepNotSupportedException();

      expect(exception, equalException);
      expect(exception.hashCode, equalException.hashCode);
    });
  });

  group('NfcNotSupportedException', () {
    test('exposes the exception message.', () {
      expect(
        const NfcNotSupportedException().message,
        'This device does not support NFC.',
      );
    });

    test(
      'includes the exception type and message in toString.',
      () => expect(
        const NfcNotSupportedException().toString(),
        'NfcNotSupportedException: This device does not support NFC.',
      ),
    );

    test('equal exceptions have equal hash codes.', () {
      const exception = NfcNotSupportedException();
      // ignore: prefer_const_constructors, verify separate instances.
      final equalException = NfcNotSupportedException();

      expect(exception, equalException);
      expect(exception.hashCode, equalException.hashCode);
    });
  });

  group('TagConnectionException', () {
    test('exposes the exception message.', () {
      expect(
        const TagConnectionException().message,
        'Failed to establish a connection with the NFC tag or the tag was '
        'lost.',
      );
    });
    test(
      'includes the exception type and message in toString.',
      () => expect(
        const TagConnectionException().toString(),
        'TagConnectionException: Failed to establish a connection with the NFC'
        ' tag or the tag was lost.',
      ),
    );

    test('equal exceptions have equal hash codes.', () {
      const exception = TagConnectionException();
      // ignore: prefer_const_constructors, verify separate instances.
      final equalException = TagConnectionException();

      expect(exception, equalException);
      expect(exception.hashCode, equalException.hashCode);
    });
  });

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
      'includes the exception type, code and message in toString.',
      () => expect(
        const NfcUnknownException(code: 'CODE', message: 'message').toString(),
        'NfcUnknownException(CODE): message',
      ),
    );
  });

  test(
    'different exception types with equal messages are unequal.',
    () {
      const known = NfcNotSupportedException();
      final unknown = NfcUnknownException(
        code: 'NFC_NOT_SUPPORTED',
        message: known.message,
      );

      expect(known, isNot(unknown));
    },
  );
}
