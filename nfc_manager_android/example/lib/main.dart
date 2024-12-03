import 'package:flutter/material.dart';
import 'package:nfc_manager_android/nfc_manager_android.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Builder(
          builder: (context) => Column(
            children: [
              TextButton(
                onPressed: () async {
                  await NfcManagerAndroidPlatform().stopDiscovery();
                },
                child: const Text('STOP'),
              ),
              StreamBuilder<Tag>(
                stream: NfcManagerAndroidPlatform().startDiscovery(),
                builder: (context, snapshot) => Column(
                  children: [
                    Text(snapshot.data?.call() ?? 'No tag'),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
