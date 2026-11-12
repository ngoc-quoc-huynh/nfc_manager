import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

extension MethodChannelExtension on MethodChannel {
  void setMockResponse<T>(T response) =>
      setMockCallHandler((_) async => response);

  void setMockCallHandler(Future<Object?> Function(MethodCall call) handler) =>
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(this, handler);

  void clearMockCallHandler() => TestDefaultBinaryMessengerBinding
      .instance
      .defaultBinaryMessenger
      .setMockMethodCallHandler(this, null);
}

extension EventChannelExtension on EventChannel {
  void setMockStream(
    void Function(MockStreamHandlerEventSink events) handler,
  ) => setMockListener((_, events) => handler(events));

  void setMockListener(
    void Function(Object? arguments, MockStreamHandlerEventSink events)
    onListen, {
    void Function(Object? arguments)? onCancel,
  }) => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockStreamHandler(
        this,
        MockStreamHandler.inline(onListen: onListen, onCancel: onCancel),
      );

  void clearMockStream() => TestDefaultBinaryMessengerBinding
      .instance
      .defaultBinaryMessenger
      .setMockStreamHandler(this, null);
}
