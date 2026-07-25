import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Base class for all NFC exceptions.
@immutable
sealed class NfcException implements Exception {
  const NfcException(this.message);

  /// The message associated with the exception.
  final String? message;

  /// Constructs an [NfcException] from a [PlatformException].
  static NfcException fromPlatformException(PlatformException exception) =>
      switch (exception.code) {
        'INVALID_APDU_COMMAND' => const InvalidApduCommandException(),
        'INVALID_LC_DATA_LENGTH' => const InvalidLcDataLengthException(),
        'ISO_DEP_NOT_SUPPORTED' => const IsoDepNotSupportedException(),
        'NFC_NOT_SUPPORTED' => const NfcNotSupportedException(),
        'TAG_CONNECTION_FAILED' => const TagConnectionException(),
        _ => NfcUnknownException(exception.message),
      };

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is NfcException &&
          runtimeType == other.runtimeType &&
          message == other.message;

  @override
  int get hashCode => message.hashCode;
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

/// Exception thrown when the exception is unknown.
final class NfcUnknownException extends NfcException {
  /// Constructs a [NfcUnknownException].
  const NfcUnknownException(super.message);

  @override
  String toString() => 'NfcUnknownException: $message';
}
