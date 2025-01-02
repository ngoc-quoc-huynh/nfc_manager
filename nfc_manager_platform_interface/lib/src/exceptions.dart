import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

@immutable
sealed class NfcException implements Exception {
  const NfcException(this.message);

  final String? message;

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

final class InvalidApduCommandException extends NfcException {
  const InvalidApduCommandException()
      : super('APDU command must be at least 4 bytes long.');

  @override
  String toString() => 'InvalidApduCommandException: $message';
}

final class InvalidLcDataLengthException extends NfcException {
  const InvalidLcDataLengthException()
      : super(
          'The LC value does not match the length of the Data field in the '
          'APDU command.',
        );

  @override
  String toString() => 'InvalidLcDataLengthException: $message';
}

final class IsoDepNotSupportedException extends NfcException {
  const IsoDepNotSupportedException()
      : super('The NFC tag does not support ISO-DEP.');

  @override
  String toString() => 'IsoDepNotSupportedException: $message';
}

final class NfcNotSupportedException extends NfcException {
  const NfcNotSupportedException() : super('This device does not support NFC.');

  @override
  String toString() => 'NfcNotSupportedException: $message';
}

final class TagConnectionException extends NfcException {
  const TagConnectionException()
      : super(
          'Failed to establish a connection with the NFC tag or the tag was '
          'lost.',
        );

  @override
  String toString() => 'TagConnectionException: $message';
}

final class NfcUnknownException extends NfcException {
  const NfcUnknownException(super.message);

  @override
  String toString() => 'NfcUnknownException: $message';
}
