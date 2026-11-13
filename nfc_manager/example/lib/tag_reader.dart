import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/demo_credentials.dart';

class const TagReaderPage({super.key}) extends StatefulWidget {
  @override
  State<TagReaderPage> createState() => _TagReaderPageState();
}

class _TagReaderPageState extends State<TagReaderPage> {
  Stream<String>? _stream;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Tag Reader')),
      body: Column(
        children: [
          switch (_stream) {
            null => FilledButton(
              onPressed: () =>
                  setState(() => _stream = const NfcManager().startDiscovery()),
              child: const Text('Start discovery'),
            ),
            Stream<String>() => FilledButton(
              onPressed: () => setState(() => _stream = null),
              child: const Text('Stop discovery'),
            ),
          },
          const SizedBox(height: 10),
          const Divider(),
          const SizedBox(height: 10),
          StreamBuilder<String>(
            stream: _stream,
            builder: (context, snapshot) => switch (snapshot.connectionState) {
              .none => const Text(
                'Start the discovery to look out for NFC tags.',
              ),
              .waiting => const CircularProgressIndicator(),
              .active || .done when snapshot.hasError => Text(
                'Received error: ${snapshot.error}',
              ),
              .active => Column(
                children: [
                  Text('Found tag: ${snapshot.requireData}'),
                  const SizedBox(height: 10),
                  FilledButton(
                    onPressed: () => unawaited(_onSelectAid(context)),
                    child: const Text('Select aid'),
                  ),
                  const SizedBox(height: 10),
                  FilledButton(
                    onPressed: () => unawaited(_onVerifyPin(context)),
                    child: const Text('Verify pin'),
                  ),
                ],
              ),
              .done => const Text('Done'),
            },
          ),
        ],
      ),
    );
  }

  Future<void> _onSelectAid(BuildContext context) async {
    String message;

    try {
      final response = await const NfcManager().sendCommand(
        ApduCommand.selectAid(demoAid.hexToBytes()),
      );
      message = 'Select AID response: $response';
    } on NfcException catch (e) {
      message = e.toString();
    }

    if (context.mounted) {
      _showMessage(context, message);
    }
  }

  Future<void> _onVerifyPin(BuildContext context) async {
    String message;

    try {
      final response = await const NfcManager().sendCommand(
        ApduCommand.verifyPin(utf8.encode(demoPin)),
      );
      message = 'Verify PIN response: $response';
    } on NfcException catch (e) {
      message = e.toString();
    }

    if (context.mounted) {
      _showMessage(context, message);
    }
  }

  void _showMessage(BuildContext context, String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(.new(content: Text(message)));
  }
}
