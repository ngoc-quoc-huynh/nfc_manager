import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:material_ui/material_ui.dart';
import 'package:nfc_manager_android/nfc_manager_android.dart';
import 'package:nfc_manager_platform_interface/nfc_manager_platform_interface.dart';

/// Minimal host app that calls each platform method once.
///
/// The full demo lives in `nfc_manager/example`.
void main() => runApp(
  const MaterialApp(
    home: NfcExample(),
  ),
);

/// Runs the Android plugin smoke checks.
class const NfcExample({super.key}) extends StatefulWidget {
  @override
  State<NfcExample> createState() => _NfcExampleState();
}

class _NfcExampleState extends State<NfcExample> {
  late final Future<List<String>> _calls;

  @override
  void initState() {
    super.initState();
    _calls = _callEachMethod();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('NFC Manager Android')),
      body: FutureBuilder<List<String>>(
        future: _calls,
        builder: (context, snapshot) {
          if (snapshot.hasError) {
            return const Center(
              child: Text('Could not complete the NFC checks.'),
            );
          }
          final lines = snapshot.data;
          if (lines == null) {
            return const Center(child: CircularProgressIndicator());
          }
          return ListView(
            children: [for (final line in lines) ListTile(title: Text(line))],
          );
        },
      ),
    );
  }
}

const _eventTimeout = Duration(seconds: 15);

Future<List<String>> _callEachMethod() async {
  final platform = NfcManagerAndroidPlatform();
  final aid = Uint8List.fromList([0xF0, 0x00, 0x00, 0x00, 0x01]);
  final pin = utf8.encode('1234');
  // Cancelling discovery closes the tag, so keep it open until sendCommand.
  final discovery = StreamIterator(
    // The transceive timeout for each command.
    platform.startDiscovery(timeout: const Duration(seconds: 5)),
  );

  final lines = [
    'isNfcSupported: ${await platform.isNfcSupported()}',
    'isNfcEnabled: ${await platform.isNfcEnabled()}',
    'isHceSupported: ${await platform.isHceSupported()}',
  ];
  try {
    final tag = await _run(() => _nextTag(discovery));
    lines.add('startDiscovery: $tag');
    final response = await _run(
      () => platform.sendCommand(ApduCommand.selectAid(aid)),
    );
    lines.add('sendCommand: $response');
  } finally {
    await discovery.cancel();
  }

  final emulation = platform.startEmulation(aid: aid, pin: pin);
  // Takes the first status, then cancels, which stops emulation.
  final status = await _run(() => emulation.timeout(_eventTimeout).first);
  lines.add('startEmulation: $status');
  return lines;
}

/// Waits for the first tag ID, or returns `ended` if discovery finished
/// without a tag.
Future<String> _nextTag(StreamIterator<String> discovery) async {
  final found = await discovery.moveNext().timeout(_eventTimeout);
  return found ? discovery.current : 'ended';
}

Future<String> _run<T>(Future<T> Function() call) async {
  try {
    return '${await call()}';
  } on TimeoutException {
    return 'timed out';
  } on NfcException catch (e) {
    return '$e';
  }
}
