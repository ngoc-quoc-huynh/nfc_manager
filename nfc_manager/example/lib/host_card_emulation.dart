import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/demo_credentials.dart';

class const HostCardEmulationPage({
  super.key,
  final NfcManager nfcManager = const NfcManager(),
}) extends StatefulWidget {
  @override
  State<HostCardEmulationPage> createState() => _HostCardEmulationPageState();
}

class _HostCardEmulationPageState extends State<HostCardEmulationPage> {
  Stream<HostCardEmulationStatus>? _stream;

  Stream<HostCardEmulationStatus> _emulate() async* {
    yield* widget.nfcManager.startEmulation(
      aid: demoAid.hexToBytes(),
      pin: utf8.encode(demoPin),
    );
  }

  void _start() => setState(() => _stream = _emulate());

  void _stop() => setState(() => _stream = null);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Host Card Emulation')),
      body: StreamBuilder(
        stream: _stream,
        builder: (context, snapshot) => Column(
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
            const SizedBox(height: 10),
            const Divider(),
            const SizedBox(height: 10),
            switch (snapshot.connectionState) {
              .none => const Text(
                'Start the emulation to check the HCE status.',
              ),
              .waiting => const CircularProgressIndicator(),
              .active || .done when snapshot.hasError => Text(
                'Received error: ${snapshot.error}',
              ),
              .active => Text(
                snapshot.requireData.toString(),
              ),
              .done => const Text('Emulation ended.'),
            },
          ],
        ),
      ),
    );
  }
}
