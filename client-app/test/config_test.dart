import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/config.dart';

void main() {
  group('AppConfig.resolve', () {
    test(
      'without an address, reaches the computer running the Android emulator',
      () {
        final config = AppConfig.resolve(
          apiBaseUrl: '',
          platform: TargetPlatform.android,
        );

        expect(config.apiBaseUrl, 'http://10.0.2.2:8081');
      },
    );

    test('without an address, reaches localhost from the iOS simulator', () {
      final config = AppConfig.resolve(
        apiBaseUrl: '',
        platform: TargetPlatform.iOS,
      );

      expect(config.apiBaseUrl, 'http://localhost:8081');
    });

    test('uses the address it was given, without trailing slashes', () {
      final config = AppConfig.resolve(
        apiBaseUrl: ' https://api.example.com/// ',
        platform: TargetPlatform.android,
      );

      expect(config.apiBaseUrl, 'https://api.example.com');
    });

    test('refuses an address that is not http or https', () {
      for (final url in [
        'api.example.com',
        'ftp://api.example.com',
        'http://',
        'https:///v1',
      ]) {
        expect(
          () =>
              AppConfig.resolve(apiBaseUrl: url, platform: TargetPlatform.iOS),
          throwsFormatException,
          reason: url,
        );
      }
    });
  });
}
