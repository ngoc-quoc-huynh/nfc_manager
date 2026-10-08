import 'package:flutter/material.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/host_card_emulation.dart';
import 'package:nfc_manager_example/tag_reader.dart';

void main() {
  runApp(const NfcManagerExample());
}

class const NfcManagerExample({super.key}) extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NFC Manager Example',
      routes: {
        'host-card-emulation': (_) => const HostCardEmulationPage(),
        'tag-reader': (_) => const TagReaderPage(),
      },
      home: Scaffold(
        appBar: AppBar(title: const Text('NFC Manager Example')),
        body: Center(
          child: Builder(
            builder: (context) => Column(
              spacing: 10,
              children: [
                _FeatureStatusView(
                  title: 'NFC support:',
                  statusFuture: NfcManager().isNfcSupported(),
                ),
                _FeatureStatusView(
                  title: 'NFC enabled:',
                  statusFuture: NfcManager().isNfcEnabled(),
                ),
                _FeatureStatusView(
                  title: 'HCE support:',
                  statusFuture: NfcManager().isHceSupported(),
                ),
                const Divider(),
                FilledButton(
                  onPressed: () =>
                      Navigator.pushNamed(context, 'host-card-emulation'),
                  child: const Text('Host Card Emulation'),
                ),
                FilledButton(
                  onPressed: () => Navigator.pushNamed(context, 'tag-reader'),
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

class const _FeatureStatusView({
  required final String title,
  required final Future<bool> statusFuture,
}) extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Row(
      mainAxisAlignment: .center,
      spacing: 5,
      children: [
        Text(title, style: textTheme.titleMedium),
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
