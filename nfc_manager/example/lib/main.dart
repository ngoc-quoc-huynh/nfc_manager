import 'package:material_ui/material_ui.dart';
import 'package:nfc_manager/nfc_manager.dart';
import 'package:nfc_manager_example/host_card_emulation.dart';
import 'package:nfc_manager_example/tag_reader.dart';

void main() => runApp(const NfcManagerExample());

class const NfcManagerExample({
  super.key,
  final NfcManager nfcManager = const NfcManager(),
}) extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NFC Manager Example',
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
                  check: nfcManager.isNfcSupported,
                ),
                _FeatureStatusView(
                  title: 'NFC enabled:',
                  check: nfcManager.isNfcEnabled,
                ),
                _FeatureStatusView(
                  title: 'HCE support:',
                  check: nfcManager.isHceSupported,
                ),
                const Divider(),
                FilledButton(
                  onPressed: () => Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (_) =>
                          HostCardEmulationPage(nfcManager: nfcManager),
                    ),
                  ),
                  child: const Text('Host Card Emulation'),
                ),
                FilledButton(
                  onPressed: () => Navigator.of(context).push(
                    MaterialPageRoute<void>(
                      builder: (_) => TagReaderPage(nfcManager: nfcManager),
                    ),
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

class const _FeatureStatusView({
  required final String title,
  required final Future<bool> Function() check,
}) extends StatefulWidget {
  @override
  State<_FeatureStatusView> createState() => _FeatureStatusViewState();
}

class _FeatureStatusViewState extends State<_FeatureStatusView> {
  late final Future<bool> _future;

  @override
  void initState() {
    super.initState();
    _future = widget.check();
  }

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;

    return Row(
      mainAxisAlignment: .center,
      spacing: 5,
      children: [
        Text(widget.title, style: textTheme.titleMedium),
        Flexible(
          child: FutureBuilder<bool>(
            future: _future,
            builder: (context, snapshot) => switch (snapshot) {
              AsyncSnapshot(hasError: true) => Text(
                'Check failed',
                style: textTheme.bodyLarge,
              ),
              AsyncSnapshot(hasData: true, :final data) => Text(
                data.toString(),
                style: textTheme.bodyLarge,
              ),
              _ => const CircularProgressIndicator(),
            },
          ),
        ),
      ],
    );
  }
}
