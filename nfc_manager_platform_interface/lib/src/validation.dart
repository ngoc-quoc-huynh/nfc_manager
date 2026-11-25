import 'dart:typed_data';

/// Checks that [aid] contains 5–16 bytes.
///
/// For platform implementations and the `nfc_manager` facade, not for apps.
///
/// Throws [RangeError] if it does not.
void checkAidLength(Uint8List aid) =>
    RangeError.checkValueInInterval(aid.length, 5, 16, 'aid.length');

/// Checks that [pin] contains 1–255 bytes.
///
/// For platform implementations and the `nfc_manager` facade, not for apps.
///
/// Throws [RangeError] if it does not.
void checkPinLength(Uint8List pin) =>
    RangeError.checkValueInInterval(pin.length, 1, 255, 'pin.length');
