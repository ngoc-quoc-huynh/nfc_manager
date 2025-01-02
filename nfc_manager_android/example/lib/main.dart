import 'dart:async';

import 'package:flutter/material.dart';
import 'package:nfc_manager_android_example/host_card_emulation.dart';
import 'package:nfc_manager_android_example/tag_reader.dart';

void main() {
  runApp(const NfcManagerAndroidApp());
}

class NfcManagerAndroidApp extends StatelessWidget {
  const NfcManagerAndroidApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NFC Manager Android Example',
      routes: {
        'host-card-emulation': (_) => const HostCardEmulationPage(),
        'tag-reader': (_) => const TagReaderPage(),
      },
      home: Scaffold(
        appBar: AppBar(
          title: const Text('NFC Manager Android Example'),
        ),
        body: Center(
          child: Builder(
            builder: (context) => Column(
              children: [
                FilledButton(
                  onPressed: () => unawaited(
                    Navigator.pushNamed(context, 'host-card-emulation'),
                  ),
                  child: const Text('Host Card Emulation'),
                ),
                const SizedBox(height: 10),
                FilledButton(
                  onPressed: () => unawaited(
                    Navigator.pushNamed(context, 'tag-reader'),
                  ),
                  child: const Text('Tag Reader'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
