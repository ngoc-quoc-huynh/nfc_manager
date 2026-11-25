import 'package:flutter/foundation.dart';

/// Base class for all NFC exceptions.
@immutable
sealed class NfcException implements Exception {
  const NfcException(this.message);

  /// A readable description for logs. It may change between versions; match
  /// on the type or [NfcUnknownException.code] instead.
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

/// Exception thrown when the platform rejects an APDU command, for example
/// because it is too long. The tag stays connected.
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

/// Exception thrown when the device fails to connect to the NFC tag, or the
/// exchange with the tag fails.
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
/// Each platform implementation defines its [code] values. See its package
/// documentation. Treat [message] as untrusted input.
final class NfcUnknownException extends NfcException {
  /// Constructs an [NfcUnknownException].
  const NfcUnknownException({required this.code, String? message})
    : super(message);

  /// The error code reported by the platform implementation.
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
