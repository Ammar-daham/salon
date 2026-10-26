import 'package:flutter/widgets.dart';

import 'app.dart';
import 'config.dart';

void main() {
  runApp(SalonatApp(config: AppConfig.fromEnvironment()));
}
