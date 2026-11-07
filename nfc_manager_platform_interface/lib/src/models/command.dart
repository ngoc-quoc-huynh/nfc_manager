import 'package:flutter/foundation.dart';

/// An immutable short Application Protocol Data Unit (APDU) command.
///
/// Use [Command.selectAid] and [Command.verifyPin] for common commands, or the
/// [Command] constructor for other header values or expected response lengths.
@immutable
final class Command {
  /// Creates a command and copies its payload.
  ///
  /// Header bytes must be 0–255. [le], when provided, must be 0–255.
  /// If provided, [data] must contain 1–255 bytes. [lc] is derived
  /// from its length and is null when data is absent.
  /// Throws [RangeError] if these requirements are not met.
  Command({
    required this.cla,
    required this.ins,
    required this.p1,
    required this.p2,
    Uint8List? data,
    this.le,
  }) : data = _copyData(data) {
    RangeError.checkValueInInterval(cla, 0, 255, 'cla');
    RangeError.checkValueInInterval(ins, 0, 255, 'ins');
    RangeError.checkValueInInterval(p1, 0, 255, 'p1');
    RangeError.checkValueInInterval(p2, 0, 255, 'p2');
    if (le case final value?) {
      RangeError.checkValueInInterval(value, 0, 255, 'le');
    }
  }

  /// Creates a SELECT command for an Application Identifier (AID).
  ///
  /// Uses the header `00 A4 04 00`, includes [aid] as the payload, and omits
  /// [le]. The AID must contain 5–16 bytes. The payload is copied.
  ///
  /// Throws [RangeError] if the AID length is outside that range.
  factory Command.selectAid(Uint8List aid) {
    RangeError.checkValueInInterval(aid.length, 5, 16, 'aid.length');
    return Command(
      cla: 0x00,
      ins: 0xA4,
      p1: 0x04,
      p2: 0x00,
      data: aid,
    );
  }

  /// Creates a VERIFY command carrying a Personal Identification Number
  /// (PIN).
  ///
  /// Uses the header `00 20 00 00`, includes [pin] as the payload, and omits
  /// [le]. The PIN must contain 1–255 bytes and use the encoding expected by
  /// the target card. The payload is copied.
  ///
  /// Throws [RangeError] if the PIN length is outside that range.
  factory Command.verifyPin(Uint8List pin) =>
      Command(cla: 0x00, ins: 0x20, p1: 0x00, p2: 0x00, data: pin);

  /// The command class byte (CLA).
  final int cla;

  /// The instruction byte (INS).
  final int ins;

  /// The first instruction parameter byte (P1).
  final int p1;

  /// The second instruction parameter byte (P2).
  final int p2;

  /// The payload length (Lc), present only when [data] is provided.
  int? get lc => data?.length;

  /// A copied, unmodifiable payload, or null for a command without data.
  final Uint8List? data;

  /// The encoded expected response data length (Le), or null when omitted.
  ///
  /// In a short APDU, zero encodes an expected length of 256 bytes.
  final int? le;

  /// Returns an independent, unmodifiable copy of [data], or null if absent.
  ///
  /// Throws [RangeError] if [data] contains fewer than 1 or more than
  /// 255 bytes.
  static Uint8List? _copyData(Uint8List? data) {
    if (data == null) {
      return null;
    }
    RangeError.checkValueInInterval(data.length, 1, 255, 'data.length');
    return Uint8List.fromList(data).asUnmodifiableView();
  }

  /// Serializes the command into a new [Uint8List].
  ///
  /// Includes [lc] only with [data], and [le] only when provided.
  /// Modifying the returned buffer does not change this command.
  Uint8List toUint8List() => Uint8List.fromList([
    cla,
    ins,
    p1,
    p2,
    ?lc,
    ...?data,
    ?le,
  ]);

  /// Commands are equal when their header fields, [data] contents, and [le]
  /// are equal.
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Command &&
          cla == other.cla &&
          ins == other.ins &&
          p1 == other.p1 &&
          p2 == other.p2 &&
          listEquals(data, other.data) &&
          le == other.le;

  @override
  int get hashCode => Object.hash(
    cla,
    ins,
    p1,
    p2,
    Object.hashAll(data ?? const <int>[]),
    le,
  );
}
