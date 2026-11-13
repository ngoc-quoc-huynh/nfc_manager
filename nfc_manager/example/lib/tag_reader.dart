import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/demo_credentials.dart';

class const TagReaderPage({
  super.key,
  final NfcManager nfcManager = const NfcManager(),
}) extends StatefulWidget {
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
                  setState(() => _stream = widget.nfcManager.startDiscovery()),
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
              .active when snapshot.hasError => Text(
                'Received error: ${snapshot.error}',
              ),
              .active => Column(
                children: [
                  Text('Found tag: ${snapshot.requireData}'),
                  const SizedBox(height: 10),
                  FilledButton(
                    onPressed: () => unawaited(
                      _send(
                        context,
                        'Select AID',
                        ApduCommand.selectAid(demoAid.hexToBytes()),
                      ),
                    ),
                    child: const Text('Select aid'),
                  ),
                  const SizedBox(height: 10),
                  FilledButton(
                    onPressed: () => unawaited(
                      _send(
                        context,
                        'Verify PIN',
                        ApduCommand.verifyPin(utf8.encode(demoPin)),
                      ),
                    ),
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

  Future<void> _send(
    BuildContext context,
    String label,
    ApduCommand command,
  ) async {
    String message;

    try {
      final response = await widget.nfcManager.sendCommand(command);
      message = '$label response: $response';
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
