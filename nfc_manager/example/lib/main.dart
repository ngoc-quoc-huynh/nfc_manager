import 'dart:async';

import 'package:flutter/material.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/host_card_emulation.dart';
import 'package:nfc_manager_example/tag_reader.dart';

void main() {
  runApp(const NfcManagerExample());
}

class NfcManagerExample extends StatelessWidget {
  const NfcManagerExample({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NFC Manager Example',
      routes: {
        'host-card-emulation': (_) => const HostCardEmulationPage(),
        'tag-reader': (_) => const TagReaderPage(),
      },
      home: Scaffold(
        appBar: AppBar(
          title: const Text('NFC Manager Example'),
        ),
        body: Center(
          child: Builder(
            builder: (context) => Column(
              spacing: 10,
              children: [
                _FeatureStatusView(
                  title: 'NFC support:',
                  // ignore: discarded_futures, future has to be unawaited.
                  statusFuture: NfcManager().isNfcSupported(),
                ),
                _FeatureStatusView(
                  title: 'NFC enabled:',
                  // ignore: discarded_futures, future has to be unawaited.
                  statusFuture: NfcManager().isNfcEnabled(),
                ),
                _FeatureStatusView(
                  title: 'HCE support:',
                  // ignore: discarded_futures, future has to be unawaited.
                  statusFuture: NfcManager().isHceSupported(),
                ),
                const Divider(),
                FilledButton(
                  onPressed: () => unawaited(
                    Navigator.pushNamed(context, 'host-card-emulation'),
                  ),
                  child: const Text('Host Card Emulation'),
                ),
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

class _FeatureStatusView extends StatelessWidget {
  const _FeatureStatusView({
    required this.title,
    required this.statusFuture,
  });

  final String title;
  final Future<bool> statusFuture;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      spacing: 5,
      children: [
        Text(
          title,
          style: textTheme.titleMedium,
        ),
        FutureBuilder<bool>(
          future: statusFuture,
          builder: (context, snapshot) => switch (snapshot.hasData) {
            false => const CircularProgressIndicator(),
            true => Text(
                snapshot.requireData.toString(),
                style: textTheme.bodyLarge,
              ),
          },
        ),
      ],
    );
  }
}
