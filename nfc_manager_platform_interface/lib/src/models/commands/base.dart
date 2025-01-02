import 'package:collection/collection.dart';
import 'package:flutter/foundation.dart';

export 'select_aid.dart';
export 'verify_pin.dart';

@immutable
base class Command {
  const Command({
    required this.cla,
    required this.ins,
    required this.p1,
    required this.p2,
    this.lc,
    this.data,
    this.le,
  });

  final int cla;
  final int ins;
  final int p1;
  final int p2;
  final int? lc;
  final Uint8List? data;
  final int? le;

  static const _listEquality = ListEquality<int>();

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
