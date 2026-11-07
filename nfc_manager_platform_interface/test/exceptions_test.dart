import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/src/exceptions.dart';

void main() {
  group('NfcException', () {
    test(
      'different exception types are unequal even with the same message.',
      () {
        const exception = NfcNotSupportedException();
        final unknown = NfcUnknownException(exception.message);

        expect(exception, isNot(unknown));
        expect(unknown, isNot(exception));
      },
    );

    test('maps known platform error codes to NFC exceptions.', () {
      expect(
        NfcException.fromPlatformException(
          PlatformException(code: 'INVALID_APDU_COMMAND'),
        ),
        const InvalidApduCommandException(),
      );
      expect(
        NfcException.fromPlatformException(
          PlatformException(code: 'INVALID_LC_DATA_LENGTH'),
        ),
        const InvalidLcDataLengthException(),
      );
      expect(
        NfcException.fromPlatformException(
          PlatformException(code: 'ISO_DEP_NOT_SUPPORTED'),
        ),
        const IsoDepNotSupportedException(),
      );
      expect(
        NfcException.fromPlatformException(
          PlatformException(code: 'NFC_NOT_SUPPORTED'),
        ),
        const NfcNotSupportedException(),
      );
      expect(
        NfcException.fromPlatformException(
          PlatformException(code: 'TAG_CONNECTION_FAILED'),
        ),
        const TagConnectionException(),
      );
    });

    test('preserves the message for unknown platform error codes.', () {
      expect(
        NfcException.fromPlatformException(PlatformException(code: '')),
        const NfcUnknownException(null),
      );
      expect(
        NfcException.fromPlatformException(
          PlatformException(code: 'unknown', message: 'message'),
        ),
        const NfcUnknownException('message'),
      );
    });
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
    test('equal messages produce equal exceptions and hash codes.', () {
      for (final message in <String?>[null, 'message']) {
        final exception = NfcUnknownException(message);
        final equalException = NfcUnknownException(message);

        expect(exception, equalException, reason: 'message: $message');
        expect(
          exception.hashCode,
          equalException.hashCode,
          reason: 'message: $message',
        );
      }
    });

    test('different messages produce unequal exceptions.', () {
      expect(
        const NfcUnknownException('first'),
        isNot(const NfcUnknownException('second')),
      );
      expect(
        const NfcUnknownException(null),
        isNot(const NfcUnknownException('')),
      );
    });

    test('exposes the exception message.', () {
      expect(const NfcUnknownException('message').message, 'message');
    });
    test(
      'includes the exception type and message in toString.',
      () => expect(
        const NfcUnknownException('message').toString(),
        'NfcUnknownException: message',
      ),
    );
  });
}
