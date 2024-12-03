import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

extension EventChannelExtension on EventChannel {
  void setMockStream(void Function(MockStreamHandlerEventSink) sink) =>
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockStreamHandler(
        this,
        MockStreamHandler.inline(
          onListen: (arguments, events) {
            sink.call(events);
            events.endOfStream();
          },
        ),
      );
}

extension MethodChannelExtension on MethodChannel {
  void setMockResponse<T>(List<MethodCall> logs, T response) =>
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        this,
        (methodCall) async {
          logs.add(methodCall);

          if (response is Exception) {
            throw response;
          }

          return response;
        },
      );
}
