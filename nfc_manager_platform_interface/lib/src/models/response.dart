import 'package:flutter/foundation.dart';
import 'package:nfc_manager_platform_interface/src/extensions.dart';

/// An immutable Application Protocol Data Unit (APDU) response: optional
/// [data] followed by the status bytes [sw1] and [sw2].
@immutable
final class ApduResponse {
  const ApduResponse._(this.data, this.sw1, this.sw2);

  /// Decodes a raw response: the response data followed by SW1 and SW2.
  ///
  /// Throws [FormatException] if [bytes] contains fewer than 2 bytes. The
  /// exception never contains the response, which may hold sensitive data.
  factory ApduResponse.fromUint8List(Uint8List bytes) {
    final length = bytes.length;
    if (length < 2) {
      throw const FormatException(
        'APDU response must contain at least the 2 status bytes.',
      );
    }
    return ApduResponse._(
      bytes.sublist(0, length - 2).asUnmodifiableView(),
      bytes[length - 2],
      bytes[length - 1],
    );
  }

  /// The unmodifiable response data, empty when the card returned only the
  /// status bytes.
  final Uint8List data;

  /// The first status byte (SW1).
  final int sw1;

  /// The second status byte (SW2).
  final int sw2;

  /// The status word SW1-SW2 as a 16-bit value, for example `0x9000`.
  int get statusWord => (sw1 << 8) | sw2;

  /// Whether the status word is `90 00`, normal processing.
  ///
  /// Other normal-processing words, such as `61 XX`, are not success.
  bool get isSuccess => statusWord == 0x9000;

  /// The classification of [statusWord], or [ApduStatus.unknown].
  ApduStatus get status => switch ((sw1, sw2)) {
    (0x90, 0x00) => .success,
    (0x61, _) => .moreDataAvailable,
    (0x63, 0x00) || (0x63, >= 0xC0 && <= 0xCF) => .verificationFailed,
    (0x67, 0x00) => .wrongLength,
    (0x69, 0x82) => .securityStatusNotSatisfied,
    (0x69, 0x83) => .authenticationMethodBlocked,
    (0x69, 0x85) => .conditionsOfUseNotSatisfied,
    (0x6A, 0x00) || (0x6A, 0x86) => .wrongParameters,
    (0x6A, 0x81) => .functionNotSupported,
    (0x6A, 0x82) => .fileOrApplicationNotFound,
    (0x6C, _) => .wrongLeField,
    (0x6D, 0x00) => .instructionNotSupported,
    _ => .unknown,
  };

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ApduResponse &&
          sw1 == other.sw1 &&
          sw2 == other.sw2 &&
          listEquals(data, other.data);

  @override
  int get hashCode => Object.hash(Object.hashAll(data), sw1, sw2);

  /// Shows the [data] length but not its contents, which may be sensitive.
  @override
  String toString() =>
      'ApduResponse(data: ${data.length} bytes, '
      'statusWord: ${Uint8List.fromList([sw1, sw2]).toHex()}, '
      'status: ${status.name})';
}

/// The ISO/IEC 7816-4 status words that [ApduResponse.status] recognizes.
enum ApduStatus {
  /// `90 00`: normal processing, no further qualification.
  success,

  /// `61 XX`: normal processing, more response data is available. SW2 is the
  /// number of bytes left; read them with GET RESPONSE (`00 C0 00 00 XX`).
  moreDataAvailable,

  /// `63 00` or `63 CX`: verification, such as a PIN check, failed. In
  /// `63 CX`, X is the number of retries left.
  verificationFailed,

  /// `67 00`: wrong length, no further indication.
  wrongLength,

  /// `69 82`: security status not satisfied.
  securityStatusNotSatisfied,

  /// `69 83`: authentication method blocked.
  authenticationMethodBlocked,

  /// `69 85`: conditions of use not satisfied.
  conditionsOfUseNotSatisfied,

  /// `6A 00` or `6A 86`: wrong parameters P1-P2, with no information given
  /// or incorrect P1-P2.
  wrongParameters,

  /// `6A 81`: function not supported.
  functionNotSupported,

  /// `6A 82`: file or application not found, for example an unknown AID.
  fileOrApplicationNotFound,

  /// `6C XX`: wrong Le field. SW2 is the exact length available, and `00`
  /// means 256 bytes.
  wrongLeField,

  /// `6D 00`: instruction code not supported or invalid.
  instructionNotSupported,

  /// Any status word not listed above.
  unknown,
}
