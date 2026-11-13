import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:nfc_manager_android/nfc_manager_android.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// Minimal host app that calls each platform method once.
///
/// The full demo lives in `nfc_manager/example`.
void main() {
  runApp(const MaterialApp(home: _Calls()));
}

class const _Calls() extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('NFC Manager Android')),
      body: FutureBuilder<List<String>>(
        future: _callEachMethod(),
        builder: (context, snapshot) => switch (snapshot.data) {
          null => const Center(child: CircularProgressIndicator()),
          final lines => ListView(
            children: [for (final line in lines) ListTile(title: Text(line))],
          ),
        },
      ),
    );
  }
}

Future<List<String>> _callEachMethod() async {
  final platform = NfcManagerAndroidPlatform();
  final aid = Uint8List.fromList([0xF0, 0x00, 0x00, 0x00, 0x01]);
  final pin = Uint8List.fromList([1, 2, 3, 4]);
  final discovery = platform.startDiscovery(
    timeout: const Duration(seconds: 5),
  );
  final emulation = platform.startEmulation(aid: aid, pin: pin);

  return [
    'isNfcSupported: ${await platform.isNfcSupported()}',
    'isNfcEnabled: ${await platform.isNfcEnabled()}',
    'isHceSupported: ${await platform.isHceSupported()}',
    'startDiscovery: ${await _run(() => discovery.first)}',
    'sendCommand: ${await _run(
      () => platform.sendCommand(ApduCommand.selectAid(aid)),
    )}',
    'startEmulation: ${await _run(() => emulation.first)}',
  ];
}

Future<String> _run<T>(Future<T> Function() call) async {
  try {
    return '${await call()}';
  } on NfcException catch (e) {
    return '$e';
  }
}
