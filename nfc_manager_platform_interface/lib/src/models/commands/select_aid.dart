import 'dart:typed_data';

import 'package:nfc_manager_platform_interface/src/models/commands/base.dart';

/// Represents an APDU command for selecting an AID (Application Identifier).
final class SelectAidCommand extends Command {
  /// Constructs a [SelectAidCommand] with the specified AID.
  const SelectAidCommand(Uint8List aid)
    : super(
        cla: 0x00,
        ins: 0xA4,
        p1: 0x04,
        p2: 0x00,
        lc: aid.length,
        data: aid,
      );
}
