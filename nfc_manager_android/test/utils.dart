import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

extension MethodChannelExtension on MethodChannel {
  void setMockResponse<T>(T response) =>
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        this,
        (_) async => response,
      );
}

extension EventChannelExtension on EventChannel {
  void setMockStream(
    void Function(MockStreamHandlerEventSink events) handler,
  ) =>
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(
        this,
        MockStreamHandler.inline(
          onListen: (arguments, events) => handler(events),
        ),
      );
}
