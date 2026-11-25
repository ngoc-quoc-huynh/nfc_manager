import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

TestDefaultBinaryMessenger get _messenger =>
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

extension MethodChannelExtension on MethodChannel {
  void setMockResponse(Object? response) =>
      setMockCallHandler((_) async => response);

  void setMockCallHandler(Future<Object?> Function(MethodCall call) handler) =>
      _messenger.setMockMethodCallHandler(this, handler);

  void clearMockCallHandler() =>
      _messenger.setMockMethodCallHandler(this, null);
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
