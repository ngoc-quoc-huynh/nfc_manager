import 'package:flutter/foundation.dart';
import 'package:nfc_manager_platform_interface/src/validation.dart';

/// An immutable short Application Protocol Data Unit (APDU) command.
///
/// Use [ApduCommand.selectAid] and [ApduCommand.verifyPin] for common
/// commands, or the [ApduCommand] constructor for other header values or
/// expected response lengths.
@immutable
final class ApduCommand {
  /// Creates a command and copies its payload.
  ///
  /// Header bytes and [le] must be 0–255. [data], if provided, must contain
  /// 1–255 bytes; [lc] is its length.
  ///
  /// Throws [RangeError] if these requirements are not met.
  ApduCommand({
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
  /// Uses the header `00 A4 04 00` with [aid] as the payload and no Le.
  /// The AID must contain 5–16 bytes.
  ///
  /// Throws [RangeError] if the AID length is outside that range.
  factory ApduCommand.selectAid(Uint8List aid) {
    checkAidLength(aid);
    return ApduCommand(
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
  /// Uses the header `00 20 00 <p2>` with [pin] as the payload and no Le.
  ///
  /// [p2] selects which PIN the card checks. The default `00` means no
  /// reference is given; many cards need a specific one, such as `80`.
  /// The PIN must contain 1–255 bytes and use the encoding expected by the
  /// target card.
  ///
  /// Throws [RangeError] if [pin] is not 1–255 bytes or [p2] is not 0–255.
  factory ApduCommand.verifyPin(Uint8List pin, {int p2 = 0x00}) {
    checkPinLength(pin);
    return ApduCommand(cla: 0x00, ins: 0x20, p1: 0x00, p2: p2, data: pin);
  }

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

  /// Returns an unmodifiable copy of [data], or null if absent.
  static Uint8List? _copyData(Uint8List? data) {
    if (data == null) {
      return null;
    }
    RangeError.checkValueInInterval(data.length, 1, 255, 'data.length');
    return Uint8List.fromList(data).asUnmodifiableView();
  }

  /// Serializes the command into a new [Uint8List].
  Uint8List toUint8List() => Uint8List.fromList([
    cla,
    ins,
    p1,
    p2,
    ?lc,
    ...?data,
    ?le,
  ]);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ApduCommand &&
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
