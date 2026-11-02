import 'package:flutter/foundation.dart';

/// Settings fixed when the app is built, from `--dart-define`.
@immutable
class AppConfig {
  const AppConfig({required this.apiBaseUrl});

  /// Reads `API_BASE_URL`, given as
  /// `--dart-define=API_BASE_URL=https://api.example.com`.
  ///
  /// Without it, the app talks to salon-backend on the computer running the
  /// simulator.
  factory AppConfig.fromEnvironment() {
    const apiBaseUrl = String.fromEnvironment('API_BASE_URL');
    return AppConfig.resolve(
      apiBaseUrl: apiBaseUrl,
      platform: defaultTargetPlatform,
    );
  }

  /// [apiBaseUrl] as given, or the default for [platform] when it is empty.
  ///
  /// Throws a [FormatException] for anything but an absolute http or https
  /// address.
  factory AppConfig.resolve({
    required String apiBaseUrl,
    required TargetPlatform platform,
  }) {
    final given = apiBaseUrl.trim();
    final url = given.isEmpty ? defaultApiBaseUrl(platform) : given;
    final uri = Uri.tryParse(url);
    if (uri == null ||
        !(uri.isScheme('http') || uri.isScheme('https')) ||
        uri.host.isEmpty) {
      throw FormatException(
        'API_BASE_URL must be an http or https address',
        url,
      );
    }
    return AppConfig(apiBaseUrl: url.replaceFirst(RegExp(r'/+$'), ''));
  }

  /// salon-backend on the computer running the simulator, as each simulator
  /// reaches it: the Android emulator sees that computer as 10.0.2.2, and the
  /// iOS simulator shares its localhost.
  static String defaultApiBaseUrl(TargetPlatform platform) =>
      platform == TargetPlatform.android
      ? 'http://10.0.2.2:8081'
      : 'http://localhost:8081';

  /// Where salon-backend answers, without a trailing slash.
  final String apiBaseUrl;
}
