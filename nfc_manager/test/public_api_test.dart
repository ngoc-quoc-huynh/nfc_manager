import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager/nfc_manager.dart';

import 'hidden_names.dart';

void main() {
  group('package:nfc_manager', () {
    test('exports the app-facing APDU, status and hex types.', () {
      final command = ApduCommand.selectAid('F000000001'.hexToBytes());
      final response = ApduResponse.fromUint8List('9000'.hexToBytes());

      expect(command.data?.toHex(), 'F000000001');
      expect(response.status, ApduStatus.success);
      expect(HostCardEmulationStatus.ready.name, 'ready');
    });

    test('exports every NfcException subtype.', () {
      // The switch only compiles if the barrel exports every subtype.
      String describe(NfcException exception) => switch (exception) {
        InvalidApduCommandException() => 'invalid command',
        IsoDepNotSupportedException() => 'no ISO-DEP',
        NfcNotSupportedException() => 'no NFC',
        TagConnectionException() => 'tag lost',
        NfcUnknownException(:final code) => code,
      };

      expect(describe(const NfcUnknownException(code: 'code')), 'code');
    });

    test('does not export the platform class or input validators.', () {
      // These names resolve to hidden_names.dart only while the barrel hides
      // them.
      expect(NfcManagerPlatform, isNot(isNull));
      expect(checkAidLength, isNot(isNull));
      expect(checkPinLength, isNot(isNull));
    });
  });
}
