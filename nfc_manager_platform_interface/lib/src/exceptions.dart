import 'package:flutter/foundation.dart';
import 'package:nfc_manager_platform_interface/src/models/apdu_command.dart';

/// Base class for all NFC exceptions.
@immutable
sealed class NfcException implements Exception {
  const NfcException(this.message);

  /// The message associated with the exception.
  final String? message;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is NfcException &&
          runtimeType == other.runtimeType &&
          message == other.message;

  @override
  int get hashCode => Object.hash(runtimeType, message);
}

/// Exception thrown when the platform rejected the APDU command.
///
/// [ApduCommand] already enforces the APDU structure, so this reports a limit
/// of the platform or tag, such as a command longer than the tag accepts. The
/// tag stays connected.
final class InvalidApduCommandException extends NfcException {
  /// Constructs an [InvalidApduCommandException].
  const InvalidApduCommandException()
    : super('The platform rejected the APDU command.');

  @override
  String toString() => 'InvalidApduCommandException: $message';
}

/// Exception thrown when the NFC tag does not support ISO-DEP.
final class IsoDepNotSupportedException extends NfcException {
  /// Constructs an [IsoDepNotSupportedException].
  const IsoDepNotSupportedException()
    : super('The NFC tag does not support ISO-DEP.');

  @override
  String toString() => 'IsoDepNotSupportedException: $message';
}

/// Exception thrown when the device does not support NFC.
final class NfcNotSupportedException extends NfcException {
  /// Constructs an [NfcNotSupportedException].
  const NfcNotSupportedException() : super('This device does not support NFC.');

  @override
  String toString() => 'NfcNotSupportedException: $message';
}

/// Exception thrown when the device fails to establish a connection with the
/// NFC tag or the tag is lost.
final class TagConnectionException extends NfcException {
  /// Constructs a [TagConnectionException].
  const TagConnectionException()
    : super(
        'Failed to establish a connection with the NFC tag or the tag was '
        'lost.',
      );

  @override
  String toString() => 'TagConnectionException: $message';
}

/// Exception thrown for a platform error that has no dedicated
/// [NfcException] subtype.
///
/// [code] is the platform's error code and [message] is the platform's own
/// text, which is not translated or guaranteed to be stable. Treat it as
/// untrusted diagnostic text. The codes are defined by each platform
/// implementation; see its package documentation.
final class NfcUnknownException extends NfcException {
  /// Constructs an [NfcUnknownException] with the platform's [code] and
  /// optional [message].
  const NfcUnknownException({required this.code, String? message})
    : super(message);

  /// The platform's error code.
  final String code;

  @override
  bool operator ==(Object other) =>
      super == other && other is NfcUnknownException && code == other.code;

  @override
  int get hashCode => Object.hash(super.hashCode, code);

  @override
  String toString() => switch (message) {
    null => 'NfcUnknownException($code)',
    final message => 'NfcUnknownException($code): $message',
  };
}
