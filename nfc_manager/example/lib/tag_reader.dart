import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/demo_card.dart';

class const TagReaderPage({
  super.key,
  final NfcManager nfcManager = const NfcManager(),
}) extends StatefulWidget {
  @override
  State<TagReaderPage> createState() => _TagReaderPageState();
}

class _TagReaderPageState extends State<TagReaderPage> {
  Stream<String>? _stream;

  void _start() => setState(() => _stream = widget.nfcManager.startDiscovery());

  void _stop() => setState(() => _stream = null);

  // Shows the error type or code, not the native message, which is untrusted.
  static String _errorText(Object? error) => switch (error) {
    NfcNotSupportedException() => 'This device has no NFC.',
    IsoDepNotSupportedException() => 'This tag is not ISO-DEP. Try another.',
    TagConnectionException() => 'Tag lost. Hold it still and try again.',
    InvalidApduCommandException() => 'The command is too long for this tag.',
    NfcUnknownException(:final code) => 'NFC failed: $code',
    _ => 'NFC failed.',
  };

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tag Reader'),
      ),
      body: StreamBuilder(
        stream: _stream,
        builder: (context, snapshot) => Column(
          spacing: 10,
          children: [
            switch (snapshot.connectionState) {
              .none => FilledButton(
                onPressed: _start,
                child: const Text('Start discovery'),
              ),
              .done => FilledButton(
                onPressed: _start,
                child: const Text('Restart discovery'),
              ),
              .waiting || .active => FilledButton(
                onPressed: _stop,
                child: const Text('Stop discovery'),
              ),
            },
            const Divider(),
            switch (snapshot.connectionState) {
              .none => const Text(
                'Start the discovery to look out for NFC tags.',
              ),
              .waiting => const CircularProgressIndicator(),
              .active || .done when snapshot.hasError => Text(
                _errorText(snapshot.error),
              ),
              .active => Column(
                spacing: 10,
                children: [
                  Text('Found tag: ${snapshot.requireData}'),
                  for (final (label, command) in [
                    ('Select AID', ApduCommand.selectAid(demoAid.hexToBytes())),
                    ('Verify PIN', ApduCommand.verifyPin(utf8.encode(demoPin))),
                  ])
                    FilledButton(
                      onPressed: () =>
                          unawaited(_send(context, label, command)),
                      child: Text(label),
                    ),
                ],
              ),
              .done => const Text('Discovery ended.'),
            },
          ],
        ),
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
      message = _errorText(e);
    }

    if (context.mounted) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(.new(content: Text(message)));
    }
  }
}
