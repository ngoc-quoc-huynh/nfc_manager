import 'package:flutter/foundation.dart';

@immutable
sealed class NfcException implements Exception {
  const NfcException(this.message);

  final String? message;

  static NfcException fromCode({
    required String code,
    required String? message,
  }) =>
      switch (code) {
        'NFC_NOT_SUPPORTED' => const NfcNotSupportedException(),
        _ => NfcUnknownException(message),
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

final class NfcUnknownException extends NfcException {
  const NfcUnknownException(super.message);

  @override
  String toString() => 'NfcUnknownException: $message.';
}

final class NfcNotSupportedException extends NfcException {
  const NfcNotSupportedException() : super('This device does not support NFC');

  @override
  String toString() =>
      'NfcNotSupportedException: This device does not support NFC.';
}
