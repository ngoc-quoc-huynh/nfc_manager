import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/demo_card.dart';

class const HostCardEmulationPage({
  super.key,
  final NfcManager nfcManager = const NfcManager(),
}) extends StatefulWidget {
  @override
  State<HostCardEmulationPage> createState() => _HostCardEmulationPageState();
}

class _HostCardEmulationPageState extends State<HostCardEmulationPage> {
  Stream<HostCardEmulationStatus>? _stream;

  void _start() => setState(
    () => _stream = widget.nfcManager.startEmulation(
      aid: demoAid.hexToBytes(),
      pin: utf8.encode(demoPin),
    ),
  );

  void _stop() => setState(() => _stream = null);

  // Shows the error code, not the native message, which is untrusted.
  static String _errorText(Object? error) => switch (error) {
    NfcUnknownException(code: 'PIN_BLOCKED') =>
      'PIN blocked after 3 wrong attempts. Restart to try again.',
    NfcUnknownException(:final code) => 'Emulation failed: $code',
    _ => 'Emulation failed.',
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Host Card Emulation')),
      body: StreamBuilder(
        stream: _stream,
        builder: (context, snapshot) => Column(
          spacing: 10,
          children: [
            switch (snapshot.connectionState) {
              .none => FilledButton(
                onPressed: _start,
                child: const Text('Start emulation'),
              ),
              .done => FilledButton(
                onPressed: _start,
                child: const Text('Restart emulation'),
              ),
              .waiting || .active => FilledButton(
                onPressed: _stop,
                child: const Text('Stop emulation'),
              ),
            },
            const Divider(),
            switch (snapshot.connectionState) {
              .none => const Text(
                'Start the emulation to check the HCE status.',
              ),
              .waiting => const CircularProgressIndicator(),
              .active || .done when snapshot.hasError => Text(
                _errorText(snapshot.error),
              ),
              .active => Text(snapshot.requireData.name),
              .done => const Text('Emulation ended.'),
            },
          ],
        ),
      ),
    );
  }
}
