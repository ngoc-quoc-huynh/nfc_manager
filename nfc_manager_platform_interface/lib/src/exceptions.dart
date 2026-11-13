import 'package:flutter/foundation.dart';

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

/// Exception thrown when the APDU command is invalid because it must be at
/// least 4 bytes long.
final class InvalidApduCommandException extends NfcException {
  /// Constructs an [InvalidApduCommandException].
  const InvalidApduCommandException()
    : super('APDU command must be at least 4 bytes long.');

  @override
  String toString() => 'InvalidApduCommandException: $message';
}

/// Exception thrown when the LC value does not match the length of the Data
/// field in the APDU command.
final class InvalidLcDataLengthException extends NfcException {
  /// Constructs an [InvalidLcDataLengthException].
  const InvalidLcDataLengthException()
    : super(
        'The LC value does not match the length of the Data field in the '
        'APDU command.',
      );

  @override
  String toString() => 'InvalidLcDataLengthException: $message';
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
/// text, which is not translated or guaranteed to be stable. On Android the
/// codes include:
///
/// * `INVALID_ARGUMENTS`: the plugin received missing or mistyped arguments.
/// * `NFC_ERROR`: the NFC reader failed unexpectedly.
/// * `UNKNOWN_HCE_STATUS`: the HCE stream received an event name this
///   version does not know; [message] holds that name.
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
  String toString() => 'NfcUnknownException($code): $message';
}
