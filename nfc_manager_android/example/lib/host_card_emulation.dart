import 'package:flutter/material.dart';
import 'package:nfc_manager_android/nfc_manager_android.dart';

class HostCardEmulationPage extends StatefulWidget {
  const HostCardEmulationPage({super.key});

  @override
  State<HostCardEmulationPage> createState() => _HostCardEmulationPageState();
}

class _HostCardEmulationPageState extends State<HostCardEmulationPage> {
  Stream<HostCardEmulationStatus>? _stream;

  static const _aid = 'F000000001';
  static const _pin = '1234';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Host Card Emulation')),
      body: Column(
        children: [
          switch (_stream) {
            null => FilledButton(
              onPressed:
                  () => setState(
                    () =>
                        _stream = NfcManagerAndroidPlatform().startEmulation(
                          aid: _aid.toUint8List(isHex: true),
                          pin: _pin.toHexString().toUint8List(),
                        ),
                  ),
              child: const Text('Start emulation'),
            ),
            Stream<HostCardEmulationStatus>() => FilledButton(
              onPressed: () => setState(() => _stream = null),
              child: const Text('Stop emulation'),
            ),
          },
          const SizedBox(height: 10),
          const Divider(),
          const SizedBox(height: 10),
          StreamBuilder(
            stream: _stream,
            builder:
                (context, snapshot) => switch (snapshot.connectionState) {
                  ConnectionState.none => const Text(
                    'Start the emulation to check the HCE status.',
                  ),
                  ConnectionState.waiting => const CircularProgressIndicator(),
                  ConnectionState.active when snapshot.hasError => Text(
                    'Received error: ${snapshot.error}',
                  ),
                  ConnectionState.active => Text(
                    snapshot.requireData.toString(),
                  ),
                  ConnectionState.done => const Text('Done'),
                },
          ),
        ],
      ),
    );
  }
}
