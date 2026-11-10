import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app/router.dart';
import 'design/design.dart';

/// Salonat, the app clients book with. Needs a [ProviderScope] above it that gives the config.
class SalonatApp extends ConsumerWidget {
  const SalonatApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp.router(
      title: 'Salonat',
      debugShowCheckedModeBanner: false,
      theme: salonatTheme(),
      routerConfig: ref.watch(routerProvider),
    );
  }
}
