import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nfc_manager_android/src/messages.g.dart';

TestDefaultBinaryMessenger get _messenger =>
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

const _hostApiMethods = [
  'isHceSupported',
  'isNfcSupported',
  'isNfcEnabled',
  'sendCommand',
];

BasicMessageChannel<Object?> _hostApiChannel(String method) =>
    BasicMessageChannel(
      'dev.flutter.pigeon.nfc_manager_android.NfcManagerHostApi.$method',
      NfcManagerHostApi.pigeonChannelCodec,
    );

/// Answers requests to the Android request API [method] on the wire.
///
/// [handler] receives the encoded arguments, `null` or a list, and returns
/// `[value]` for a result or `[code, message, details]` for an error.
void setMockHostApiHandler(
  String method,
  List<Object?>? Function(Object? message) handler,
) => _messenger.setMockDecodedMessageHandler<Object?>(
  _hostApiChannel(method),
  (message) async => handler(message),
);

void clearMockHostApiHandlers() {
  for (final method in _hostApiMethods) {
    _messenger.setMockDecodedMessageHandler(_hostApiChannel(method), null);
  }
}

extension EventChannelExtension on EventChannel {
  void setMockStream(
    void Function(MockStreamHandlerEventSink events) onListen,
  ) => setMockListener((_, events) => onListen(events));

  void setMockListener(
    void Function(Object? arguments, MockStreamHandlerEventSink events)
    onListen, {
    void Function(Object? arguments)? onCancel,
  }) => _messenger.setMockStreamHandler(
    this,
    MockStreamHandler.inline(onListen: onListen, onCancel: onCancel),
  );

  void clearMockStream() => _messenger.setMockStreamHandler(this, null);
}
