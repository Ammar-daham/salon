import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'app/providers.dart';
import 'config.dart';

void main() {
  // Outfit is bundled with the app, so its licence goes on the app's licences page.
  LicenseRegistry.addLicense(() async* {
    final licence = await rootBundle.loadString('assets/fonts/outfit/OFL.txt');
    yield LicenseEntryWithLineBreaks(['Outfit'], licence);
  });
  runApp(
    ProviderScope(
      overrides: [
        configProvider.overrideWithValue(AppConfig.fromEnvironment()),
      ],
      child: const SalonatApp(),
    ),
  );
}
