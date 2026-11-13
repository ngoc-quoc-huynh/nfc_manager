import 'dart:typed_data';

/// Checks that [aid] is a valid Application Identifier (AID) length.
///
/// An AID must contain 5-16 bytes. This check is for platform
/// implementations and the `nfc_manager` facade, not for apps.
///
/// Throws [RangeError] named `aid.length` if the length is outside that range.
void checkAidLength(Uint8List aid) {
  RangeError.checkValueInInterval(aid.length, 5, 16, 'aid.length');
}

/// Checks that [pin] is a valid Personal Identification Number (PIN) length.
///
/// A PIN must contain 1-255 bytes. This check is for platform
/// implementations and the `nfc_manager` facade, not for apps.
///
/// Throws [RangeError] named `pin.length` if the length is outside that range.
void checkPinLength(Uint8List pin) {
  RangeError.checkValueInInterval(pin.length, 1, 255, 'pin.length');
}
