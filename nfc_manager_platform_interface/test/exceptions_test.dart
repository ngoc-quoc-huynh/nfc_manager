import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/src/exceptions.dart';

void main() {
  group('NfcException', () {
    test('fromCode returns correctly.', () {
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

    test('fromCode returns NfcUnknownException for unknown code.', () {
      expect(
        NfcException.fromPlatformException(
          PlatformException(code: ''),
        ),
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
    test(
      'message returns correctly.',
      () {
        expect(
          const InvalidApduCommandException().message,
          'APDU command must be at least 4 bytes long.',
        );
        expect(
          // ignore: prefer_const_constructors, for coverage.
          InvalidApduCommandException().message,
          'APDU command must be at least 4 bytes long.',
        );
      },
    );

    test(
      'toString returns correctly.',
      () => expect(
        const InvalidApduCommandException().toString(),
        'InvalidApduCommandException: APDU command must be at least 4 bytes'
        ' long.',
      ),
    );

    test(
      'hashCode returns correctly.',
      () => expect(
        const InvalidApduCommandException().hashCode,
        233482670,
      ),
    );
  });

  group('InvalidLcDataLengthException', () {
    test(
      'message returns correctly.',
      () {
        expect(
          const InvalidLcDataLengthException().message,
          'The LC value does not match the length of the Data field in the APDU command.',
        );
        expect(
          // ignore: prefer_const_constructors, for coverage.
          InvalidLcDataLengthException().message,
          'The LC value does not match the length of the Data field in the APDU command.',
        );
      },
    );

    test(
      'toString returns correctly.',
      () => expect(
        const InvalidLcDataLengthException().toString(),
        'InvalidLcDataLengthException: The LC value does not match the length '
        'of the Data field in the APDU command.',
      ),
    );

    test(
      'hashCode returns correctly.',
      () => expect(
        const InvalidLcDataLengthException().hashCode,
        566604090,
      ),
    );
  });

  group('IsoDepNotSupportedException', () {
    test(
      'message returns correctly.',
      () {
        expect(
          const IsoDepNotSupportedException().message,
          'The NFC tag does not support ISO-DEP.',
        );
        expect(
          // ignore: prefer_const_constructors, for coverage.
          IsoDepNotSupportedException().message,
          'The NFC tag does not support ISO-DEP.',
        );
      },
    );

    test(
      'toString returns correctly.',
      () => expect(
        const IsoDepNotSupportedException().toString(),
        'IsoDepNotSupportedException: The NFC tag does not support ISO-DEP.',
      ),
    );

    test(
      'hashCode returns correctly.',
      () => expect(
        const IsoDepNotSupportedException().hashCode,
        169376260,
      ),
    );
  });

  group('NfcNotSupportedException', () {
    test(
      'message returns correctly.',
      () {
        expect(
          const NfcNotSupportedException().message,
          'This device does not support NFC.',
        );
        expect(
          // ignore: prefer_const_constructors, for coverage.
          NfcNotSupportedException().message,
          'This device does not support NFC.',
        );
      },
    );

    test(
      'toString returns correctly.',
      () => expect(
        const NfcNotSupportedException().toString(),
        'NfcNotSupportedException: This device does not support NFC.',
      ),
    );

    test(
      'hashCode returns correctly.',
      () => expect(
        const NfcNotSupportedException().hashCode,
        283601179,
      ),
    );
  });

  group('TagConnectionException', () {
    test(
      'message returns correctly.',
      () {
        expect(
          const TagConnectionException().message,
          'Failed to establish a connection with the NFC tag or the tag was lost.',
        );
        expect(
          // ignore: prefer_const_constructors, for coverage.
          TagConnectionException().message,
          'Failed to establish a connection with the NFC tag or the tag was lost.',
        );
      },
    );
    test(
      'toString returns correctly.',
      () => expect(
        const TagConnectionException().toString(),
        'TagConnectionException: Failed to establish a connection with the NFC'
        ' tag or the tag was lost.',
      ),
    );

    test(
      'hashCode returns correctly.',
      () => expect(
        const TagConnectionException().hashCode,
        330770710,
      ),
    );
  });

  group('NfcUnknownException', () {
    test(
      'message returns correctly.',
      () {
        expect(
          const NfcUnknownException('message').message,
          'message',
        );
        expect(
          // ignore: prefer_const_constructors, for coverage.
          NfcUnknownException('message').message,
          'message',
        );
      },
    );
    test(
      'toString returns correctly.',
      () => expect(
        const NfcUnknownException('message').toString(),
        'NfcUnknownException: message',
      ),
    );
  });
}
