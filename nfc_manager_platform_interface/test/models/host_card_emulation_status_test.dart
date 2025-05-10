import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  group('fromString', () {
    test('returns correctly.', () {
      expect(
        HostCardEmulationStatus.fromString('READY'),
        HostCardEmulationStatus.ready,
      );
      expect(
        HostCardEmulationStatus.fromString('INVALID_COMMAND'),
        HostCardEmulationStatus.invalidCommand,
      );
      expect(
        HostCardEmulationStatus.fromString('INVALID_LC_LENGTH'),
        HostCardEmulationStatus.invalidLcLength,
      );
      expect(
        HostCardEmulationStatus.fromString('AID_SELECTED'),
        HostCardEmulationStatus.aidSelected,
      );
      expect(
        HostCardEmulationStatus.fromString('INVALID_AID'),
        HostCardEmulationStatus.invalidAid,
      );
      expect(
        HostCardEmulationStatus.fromString('PIN_VERIFIED'),
        HostCardEmulationStatus.pinVerified,
      );
      expect(
        HostCardEmulationStatus.fromString('WRONG_PIN'),
        HostCardEmulationStatus.wrongPin,
      );
      expect(
        HostCardEmulationStatus.fromString('FUNCTION_NOT_SUPPORTED'),
        HostCardEmulationStatus.functionNotSupported,
      );
      expect(
        HostCardEmulationStatus.fromString('TAG_DISCONNECTED'),
        HostCardEmulationStatus.tagDisconnected,
      );
    });

    test(
      'throws ArgumentError if status does not exist.',
      () => expect(
        () => HostCardEmulationStatus.fromString('TEST'),
        throwsA(
          isA<ArgumentError>()
              .having((e) => e.invalidValue, 'invalidValue', 'TEST')
              .having(
                (e) => e.message,
                'message',
                'No enum value with that status.',
              ),
        ),
      ),
    );
  });
}
