import 'dart:async';

import 'package:flutter/material.dart';
import 'package:nfc_manager/nfc_manager.dart';

class TagReaderPage extends StatefulWidget {
  const TagReaderPage({super.key});

  @override
  State<TagReaderPage> createState() => _TagReaderPageState();
}

class _TagReaderPageState extends State<TagReaderPage> {
  Stream<String>? _stream;

  static const _aid = 'F000000001';
  static const _pin = '1234';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Tag Reader')),
      body: Column(
        children: [
          switch (_stream) {
            null => FilledButton(
              onPressed: () =>
                  setState(() => _stream = NfcManager().startDiscovery()),
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
              ConnectionState.none => const Text(
                'Start the discovery to look out for NFC tags.',
              ),
              ConnectionState.waiting => const CircularProgressIndicator(),
              ConnectionState.active when snapshot.hasError => Text(
                'Received error: ${snapshot.error}',
              ),
              ConnectionState.active => Column(
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
              ConnectionState.done => const Text('Done'),
            },
          ),
        ],
      ),
    );
  }

  Future<void> _onSelectAid(BuildContext context) async {
    String message;

    try {
      final response = await NfcManager().sendCommand(
        SelectAidCommand(_aid.toUint8List(isHex: true)),
      );
      message = 'Select AID response: $response';
    } on NfcException catch (e) {
      message = e.toString();
    }

    if (context.mounted) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(message)));
    }
  }

  Future<void> _onVerifyPin(BuildContext context) async {
    String message;
    try {
      final response = await NfcManager().sendCommand(
        VerifyPinCommand(_pin.toHexString().toUint8List()),
      );
      message = 'Verify PIN response: $response';
    } on NfcException catch (e) {
      message = e.toString();
    }

    if (context.mounted) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(message)));
    }
  }
}
