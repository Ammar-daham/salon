import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import 'config.dart';
import 'design/design.dart';
import 'design/gallery.dart';

/// Salonat, the app clients book with.
class SalonatApp extends StatelessWidget {
  const SalonatApp({super.key, required this.config});

  final AppConfig config;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Salonat',
      debugShowCheckedModeBanner: false,
      theme: salonatTheme(),
      home: StartScreen(config: config),
    );
  }
}

/// What the app shows until its first real screen, Welcome, is built.
class StartScreen extends StatelessWidget {
  const StartScreen({super.key, required this.config});

  final AppConfig config;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              spacing: 8,
              children: [
                Text('Salonat', style: text.headlineMedium),
                Text('Booking is on its way.', style: text.bodyLarge),
                // Which API a debug build was given, so a wrong
                // --dart-define is easy to spot.
                if (kDebugMode) ...[
                  Text('API: ${config.apiBaseUrl}', style: text.bodySmall),
                  const SizedBox(height: 16),
                  SalonatButton(
                    label: 'Design gallery',
                    kind: ButtonKind.secondary,
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => const DesignGallery(),
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
