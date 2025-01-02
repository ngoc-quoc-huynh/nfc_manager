import 'dart:typed_data';

import 'package:nfc_manager_platform_interface/src/models/commands/base.dart';

final class VerifyPinCommand extends Command {
  const VerifyPinCommand(Uint8List pin)
      : super(
          cla: 0x00,
          ins: 0x20,
          p1: 0x00,
          p2: 0x00,
          lc: pin.length,
          data: pin,
        );
}
