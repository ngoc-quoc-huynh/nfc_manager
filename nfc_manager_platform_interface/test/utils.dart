import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

extension MethodChannelExtension on MethodChannel {
  void setMockResponse<T>(List<MethodCall> logs, T response) =>
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
        this,
        (methodCall) async {
          logs.add(methodCall);
          return response;
        },
      );
}
