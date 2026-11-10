import 'package:flutter/foundation.dart';

/// Where the app's data comes from.
enum DataSource {
  /// The stand-ins in `lib/data`, with the demo salons: until salon-backend serves the app.
  fake,

  /// salon-backend, for what it already serves the app.
  api,
}

/// Settings fixed when the app is built, from `--dart-define`.
@immutable
class AppConfig {
  const AppConfig({
    required this.apiBaseUrl,
    this.dataSource = DataSource.fake,
  });

  /// Reads `API_BASE_URL`, given as
  /// `--dart-define=API_BASE_URL=https://api.example.com`, and `DATA`, given
  /// as `--dart-define=DATA=api`.
  ///
  /// Without them, the app uses its stand-ins, and would talk to salon-backend
  /// on the computer running the simulator.
  factory AppConfig.fromEnvironment() {
    const apiBaseUrl = String.fromEnvironment('API_BASE_URL');
    const data = String.fromEnvironment('DATA');
    return AppConfig.resolve(
      apiBaseUrl: apiBaseUrl,
      platform: defaultTargetPlatform,
      data: data,
    );
  }

  /// [apiBaseUrl] as given, or the default for [platform] when it is empty;
  /// and [data], `fake` or `api`, or [DataSource.fake] when it is empty.
  ///
  /// Throws a [FormatException] for anything but an absolute http or https
  /// address, or a data source other than those two.
  factory AppConfig.resolve({
    required String apiBaseUrl,
    required TargetPlatform platform,
    String data = '',
  }) {
    final source = switch (data.trim()) {
      '' || 'fake' => DataSource.fake,
      'api' => DataSource.api,
      _ => throw FormatException('DATA must be fake or api', data),
    };
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
    return AppConfig(
      apiBaseUrl: url.replaceFirst(RegExp(r'/+$'), ''),
      dataSource: source,
    );
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

  final DataSource dataSource;
}
