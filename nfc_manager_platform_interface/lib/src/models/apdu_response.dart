import 'package:flutter/foundation.dart';
import 'package:nfc_manager_platform_interface/src/extensions.dart';

/// An immutable Application Protocol Data Unit (APDU) response.
///
/// A response is the optional response [data] followed by the two status
/// bytes [sw1] and [sw2]. Use [isSuccess] to check for `90 00`, [status] to
/// classify the status words this plugin recognizes, and [sw2] for
/// status-word parameters such as remaining bytes or retry counters.
@immutable
final class ApduResponse {
  const ApduResponse._(this.data, this.sw1, this.sw2);

  /// Decodes a raw response: the response data followed by SW1 and SW2.
  ///
  /// [data] is an unmodifiable copy of the response payload.
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
  /// Other normal-processing words, such as `61 XX` (more data available),
  /// are not success here; check [sw1] and [sw2] for them.
  bool get isSuccess => statusWord == 0x9000;

  /// The classification of [statusWord], or [ApduStatus.unknown].
  ApduStatus get status => switch (statusWord) {
    0x9000 => .success,
    0x6300 => .verificationFailed,
    0x6700 => .wrongLength,
    0x6982 => .securityStatusNotSatisfied,
    0x6A00 => .wrongParameters,
    0x6A81 => .functionNotSupported,
    0x6A82 => .fileOrApplicationNotFound,
    0x6C00 => .wrongLeField,
    _ => .unknown,
  };

  /// Responses are equal when their [data] contents, [sw1], and [sw2] are
  /// equal.
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
///
/// Each value matches one exact status word. Any other word, including a
/// recognized SW1 with a different SW2, is [unknown]; read
/// [ApduResponse.sw1] and [ApduResponse.sw2] for those.
enum ApduStatus {
  /// `90 00`: normal processing, no further qualification.
  success,

  /// `63 00`: warning, no information given. Cards commonly return it when
  /// verification, such as a PIN check, fails. This plugin's card emulation
  /// returns it for a wrong PIN.
  verificationFailed,

  /// `67 00`: wrong length, no further indication. This plugin's card
  /// emulation returns it for a command that is too short or whose Lc does
  /// not match its data length.
  wrongLength,

  /// `69 82`: security status not satisfied. This plugin's card emulation
  /// returns it when emulation is not configured.
  securityStatusNotSatisfied,

  /// `6A 00`: wrong parameters P1-P2, no information given. This plugin's
  /// card emulation returns it when Android delivers no command.
  wrongParameters,

  /// `6A 81`: function not supported.
  functionNotSupported,

  /// `6A 82`: file or application not found, for example an unknown AID.
  fileOrApplicationNotFound,

  /// `6C 00`: wrong Le field; SW2 `00` means 256 bytes are available.
  ///
  /// Other `6C XX` words are [unknown]; their SW2 is the exact length.
  wrongLeField,

  /// Any status word not listed above.
  unknown,
}
