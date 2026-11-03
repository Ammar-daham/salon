import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'app.dart';
import 'config.dart';

void main() {
  // Outfit is bundled with the app, so its licence goes on the app's licences page.
  LicenseRegistry.addLicense(() async* {
    final licence = await rootBundle.loadString('assets/fonts/outfit/OFL.txt');
    yield LicenseEntryWithLineBreaks(['Outfit'], licence);
  });
  runApp(SalonatApp(config: AppConfig.fromEnvironment()));
}
