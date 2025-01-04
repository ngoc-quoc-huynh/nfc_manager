import 'package:collection/collection.dart';
import 'package:flutter/foundation.dart';

export 'select_aid.dart';
export 'verify_pin.dart';

/// Represents the base class for Application Protocol Data Unit (APDU) commands
/// used in NFC operations.
@immutable
base class Command {
  /// Constructs a [Command] instance using specified APDU command parameters.
  const Command({
    required this.cla,
    required this.ins,
    required this.p1,
    required this.p2,
    this.lc,
    this.data,
    this.le,
  });

  /// The class byte (CLA) of the APDU command, which identifies the category of
  /// the command.
  final int cla;

  /// The instruction byte (INS) of the APDU command, which specifies the
  /// specific operation to be performed.
  final int ins;

  /// The first parameter byte (P1) of the APDU command, used for providing
  /// further instruction specifics or conditions.
  final int p1;

  /// The second parameter byte (P2) of the APDU command, often used in
  /// conjunction with P1 to provide additional command details or requirements.
  final int p2;

  /// An optional length byte (Lc) that indicates the number of bytes in the
  /// command data field.
  /// This is only present if the command includes data.
  final int? lc;

  /// Optional data payload for the command, represented as a sequence of bytes.
  /// This field is used when the command needs to send data to a card or
  /// device.
  final Uint8List? data;

  /// An optional expected length byte (Le) that specifies the maximum number
  /// of bytes expected in the response from the card.
  /// This is not required if no response is expected.
  final int? le;

  static const _listEquality = ListEquality<int>();

  /// Serializes the command into a [Uint8List] format suitable for NFC
  /// communication.
  Uint8List toUint8List() => Uint8List.fromList(
        [
          cla,
          ins,
          p1,
          p2,
          if (lc case final lc?) lc,
          if (data case final data?) ...data,
          if (le case final le?) le,
        ],
      );

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Command &&
          runtimeType == other.runtimeType &&
          cla == other.cla &&
          ins == other.ins &&
          p1 == other.p1 &&
          p2 == other.p2 &&
          lc == other.lc &&
          _listEquality.equals(data, other.data) &&
          le == other.le;

  @override
  int get hashCode => Object.hash(
        cla,
        ins,
        p1,
        p2,
        lc,
        _listEquality.hash(data),
        le,
      );
}
