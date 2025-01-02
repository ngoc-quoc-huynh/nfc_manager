import 'dart:async';

import 'package:flutter/material.dart';
import 'package:nfc_manager_android/nfc_manager_android.dart';
import 'package:nfc_manager_android_example/host_card_emulation.dart';
import 'package:nfc_manager_android_example/tag_reader.dart';

void main() {
  runApp(const NfcManagerAndroidExample());
}

class NfcManagerAndroidExample extends StatelessWidget {
  const NfcManagerAndroidExample({super.key});

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
              spacing: 10,
              children: [
                _FeatureStatusView(
                  title: 'NFC support:',
                  // ignore: discarded_futures, future has to be unawaited.
                  statusFuture: NfcManagerAndroidPlatform().isNfcSupported(),
                ),
                _FeatureStatusView(
                  title: 'NFC enabled:',
                  // ignore: discarded_futures, future has to be unawaited.
                  statusFuture: NfcManagerAndroidPlatform().isNfcEnabled(),
                ),
                _FeatureStatusView(
                  title: 'HCE support:',
                  // ignore: discarded_futures, future has to be unawaited.
                  statusFuture: NfcManagerAndroidPlatform().isHceSupported(),
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
