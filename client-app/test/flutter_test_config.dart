import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

/// Runs before every test file. Loads Outfit, so text in tests takes the room it takes in the app
/// rather than the room of the test font's square letters.
Future<void> testExecutable(FutureOr<void> Function() testMain) async {
  TestWidgetsFlutterBinding.ensureInitialized();
  final outfit = FontLoader('Outfit');
  for (final weight in ['Regular', 'Medium', 'SemiBold']) {
    outfit.addFont(rootBundle.load('assets/fonts/outfit/Outfit-$weight.ttf'));
  }
  await outfit.load();
  await testMain();
}
