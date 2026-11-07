import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

void main() {
  group('Command', () {
    test('derives Lc from the payload and omits it without data.', () {
      final command = Command(
        cla: 0,
        ins: 1,
        p1: 2,
        p2: 3,
        data: Uint8List.fromList([4, 5]),
      );

      expect(command.lc, 2);
      expect(command.toUint8List(), [0, 1, 2, 3, 2, 4, 5]);
      expect(Command(cla: 0, ins: 1, p1: 2, p2: 3).lc, isNull);
    });

    group('validation', () {
      test('rejects header bytes and Le outside the byte range.', () {
        for (final value in [-1, 256]) {
          for (final (name, create) in <(String, Command Function())>[
            ('cla', () => Command(cla: value, ins: 0, p1: 0, p2: 0)),
            ('ins', () => Command(cla: 0, ins: value, p1: 0, p2: 0)),
            ('p1', () => Command(cla: 0, ins: 0, p1: value, p2: 0)),
            ('p2', () => Command(cla: 0, ins: 0, p1: 0, p2: value)),
            ('le', () => Command(cla: 0, ins: 0, p1: 0, p2: 0, le: value)),
          ]) {
            expect(create, throwsRangeError, reason: '$name: $value');
          }
        }
      });

      test('reports the invalid length for empty and oversized payloads.', () {
        for (final length in [0, 256]) {
          expect(
            () =>
                Command(cla: 0, ins: 0, p1: 0, p2: 0, data: Uint8List(length)),
            throwsA(
              isA<RangeError>().having(
                (error) => error.invalidValue,
                'invalidValue',
                length,
              ),
            ),
            reason: 'data length: $length',
          );
        }
      });

      test('accepts a maximum-length short payload and byte boundaries.', () {
        final command = Command(
          cla: 0,
          ins: 255,
          p1: 0,
          p2: 255,
          data: Uint8List(255),
          le: 255,
        );
        final bytes = command.toUint8List();
        expect(bytes.length, 261);
        expect(bytes.take(5), [0, 255, 0, 255, 255]);
        expect(bytes.last, 255);
      });
    });

    group('immutability', () {
      test('modifying payload buffers does not change the command.', () {
        final source = Uint8List.fromList([1]);
        final command = Command(cla: 0, ins: 0, p1: 0, p2: 0, data: source);
        final equalCommand = Command(
          cla: 0,
          ins: 0,
          p1: 0,
          p2: 0,
          data: Uint8List.fromList([1]),
        );
        final originalHash = command.hashCode;
        final commands = {command};

        source.first = 2;
        expect(command.toUint8List(), [0, 0, 0, 0, 1, 1]);
        expect(command, equalCommand);
        expect(command.hashCode, originalHash);
        expect(commands.contains(equalCommand), isTrue);
        expect(() => command.data!.first = 3, throwsUnsupportedError);
        expect(
          () => command.data!.buffer.asUint8List().first = 3,
          throwsUnsupportedError,
        );
        expect(
          () => ByteData.view(command.data!.buffer).setUint8(0, 3),
          throwsUnsupportedError,
        );

        command.toUint8List()[5] = 4;
        expect(command.toUint8List(), [0, 0, 0, 0, 1, 1]);
      });
    });

    group('toUint8List', () {
      test('preserves the order of header, length, payload, and Le bytes.', () {
        final command = Command(
          cla: 0x80,
          ins: 0x20,
          p1: 0x01,
          p2: 0x02,
          data: Uint8List.fromList([0xAB, 0xCD]),
          le: 0xEF,
        );

        expect(command.toUint8List(), [
          0x80,
          0x20,
          0x01,
          0x02,
          0x02,
          0xAB,
          0xCD,
          0xEF,
        ]);
      });

      test('serializes a short command with Le and no payload.', () {
        expect(Command(cla: 0, ins: 1, p1: 2, p2: 3, le: 0).toUint8List(), [
          0,
          1,
          2,
          3,
          0,
        ]);
      });

      test('serializes a short command with payload and no Le.', () {
        expect(
          Command(
            cla: 0,
            ins: 1,
            p1: 2,
            p2: 3,
            data: Uint8List.fromList([4]),
          ).toUint8List(),
          [0, 1, 2, 3, 1, 4],
        );
      });
      test(
        'serializes a header-only command.',
        () => expect(
          Command(cla: 0x00, ins: 0x00, p1: 0x00, p2: 0x00).toUint8List(),
          Uint8List.fromList([0x00, 0x00, 0x00, 0x00]),
        ),
      );
    });

    group('equality', () {
      test('commands with and without a payload are unequal.', () {
        final withoutPayload = Command(cla: 0, ins: 0, p1: 0, p2: 0);
        final withPayload = Command(
          cla: 0,
          ins: 0,
          p1: 0,
          p2: 0,
          data: Uint8List.fromList([0]),
        );

        expect(withoutPayload, isNot(withPayload));
        expect(withPayload, isNot(withoutPayload));
      });

      test('commands with equal fields have equal hash codes.', () {
        final command1 = Command(
          cla: 0x00,
          ins: 0x00,
          p1: 0x00,
          p2: 0x00,
          data: Uint8List.fromList([0x00]),
          le: 0x00,
        );
        final command2 = Command(
          cla: 0x00,
          ins: 0x00,
          p1: 0x00,
          p2: 0x00,
          data: Uint8List.fromList([0x00]),
          le: 0x00,
        );
        expect(command1, command2);
        expect(command1.hashCode, command2.hashCode);
      });

      test(
        'equal header-only commands have equal hash codes.',
        () {
          final command1 = Command(cla: 0x00, ins: 0x00, p1: 0x00, p2: 0x00);
          final command2 = Command(cla: 0x00, ins: 0x00, p1: 0x00, p2: 0x00);
          expect(command1, command2);
          expect(command1.hashCode, command2.hashCode);
        },
      );

      for (final (name, command) in [
        ('cla', Command(cla: 1, ins: 0, p1: 0, p2: 0)),
        ('ins', Command(cla: 0, ins: 1, p1: 0, p2: 0)),
        ('p1', Command(cla: 0, ins: 0, p1: 1, p2: 0)),
        ('p2', Command(cla: 0, ins: 0, p1: 0, p2: 1)),
      ]) {
        test('different $name values produce unequal commands.', () {
          final original = Command(cla: 0, ins: 0, p1: 0, p2: 0);
          expect(original, isNot(command));
          expect(command, isNot(original));
        });
      }

      test('different payload contents produce unequal commands.', () {
        final command1 = Command(
          cla: 0,
          ins: 0,
          p1: 0,
          p2: 0,
          data: Uint8List.fromList([1]),
        );
        final command2 = Command(
          cla: 0,
          ins: 0,
          p1: 0,
          p2: 0,
          data: Uint8List.fromList([2]),
        );
        expect(command1, isNot(command2));
      });

      test('omitted Le and zero Le produce unequal commands.', () {
        final command1 = Command(cla: 0, ins: 0, p1: 0, p2: 0);
        final command2 = Command(cla: 0, ins: 0, p1: 0, p2: 0, le: 0);
        expect(command1, isNot(command2));
        expect(command2, isNot(command1));
      });

      test('different Le bytes produce unequal commands.', () {
        final command1 = Command(cla: 0, ins: 0, p1: 0, p2: 0, le: 0);
        final command2 = Command(cla: 0, ins: 0, p1: 0, p2: 0, le: 1);
        expect(command1, isNot(command2));
      });

      test(
        'a factory command equals a generic command with the same fields.',
        () {
          final payload = Uint8List.fromList([1]);
          final command = Command.verifyPin(payload);
          final generic = Command(
            cla: 0,
            ins: 0x20,
            p1: 0,
            p2: 0,
            data: payload,
          );

          expect(command.toUint8List(), generic.toUint8List());
          expect(command, generic);
          expect(command.hashCode, generic.hashCode);
        },
      );
    });
  });

  group('Command.selectAid', () {
    test('rejects AIDs outside the 5–16 byte range.', () {
      for (final length in [0, 1, 4, 17]) {
        expect(
          () => Command.selectAid(Uint8List(length)),
          throwsA(
            isA<RangeError>()
                .having((error) => error.invalidValue, 'invalidValue', length)
                .having((error) => error.start, 'start', 5)
                .having((error) => error.end, 'end', 16)
                .having((error) => error.name, 'name', 'aid.length'),
          ),
          reason: 'AID length: $length',
        );
      }
    });

    test('accepts AIDs of 5 and 16 bytes.', () {
      for (final (length, encodedLength) in [(5, 10), (16, 21)]) {
        expect(
          Command.selectAid(Uint8List(length)).toUint8List().length,
          encodedLength,
          reason: 'AID length: $length',
        );
      }
    });

    test('serializes the SELECT AID header and payload.', () {
      final aid = Uint8List.fromList([0xF0, 0x00, 0x00, 0x00, 0x01]);
      final command = Command.selectAid(aid);
      final expected = Uint8List.fromList([
        0x00,
        0xA4,
        0x04,
        0x00,
        aid.length,
        ...aid,
      ]);

      expect(command.toUint8List(), expected);
    });
  });

  group('Command.verifyPin', () {
    test('rejects empty and oversized PIN payloads.', () {
      for (final length in [0, 256]) {
        expect(
          () => Command.verifyPin(Uint8List(length)),
          throwsRangeError,
          reason: 'PIN length: $length',
        );
      }
    });

    test('accepts PIN payloads of 1 and 255 bytes.', () {
      for (final (length, encodedLength) in [(1, 6), (255, 260)]) {
        expect(
          Command.verifyPin(Uint8List(length)).toUint8List().length,
          encodedLength,
          reason: 'PIN length: $length',
        );
      }
    });

    test('serializes the VERIFY PIN header and payload.', () {
      final pin = Uint8List.fromList([0x00]);
      final command = Command.verifyPin(pin);
      final expected = Uint8List.fromList([
        0x00,
        0x20,
        0x00,
        0x00,
        pin.length,
        ...pin,
      ]);

      expect(command.toUint8List(), expected);
    });
  });
}
