import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/app.dart';
import 'package:salonat_client/config.dart';
import 'package:salonat_client/design/gallery.dart';

void main() {
  testWidgets('starts on the start screen, showing the API it was given', (
    tester,
  ) async {
    await tester.pumpWidget(
      const SalonatApp(config: AppConfig(apiBaseUrl: 'http://localhost:8081')),
    );

    expect(find.text('Salonat'), findsOneWidget);
    expect(find.text('Booking is on its way.'), findsOneWidget);
    expect(find.text('API: http://localhost:8081'), findsOneWidget);
  });

  testWidgets('a debug build opens the design gallery from the start screen', (
    tester,
  ) async {
    await tester.pumpWidget(
      const SalonatApp(config: AppConfig(apiBaseUrl: 'http://localhost:8081')),
    );

    await tester.tap(find.text('Design gallery'));
    await tester.pumpAndSettle();

    expect(find.byType(DesignGallery), findsOneWidget);
  });
}
