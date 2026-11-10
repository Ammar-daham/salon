import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/app/router.dart';
import 'package:salonat_client/app/routes.dart';
import 'package:salonat_client/data/sample_data.dart';
import 'package:salonat_client/design/design.dart';
import 'package:salonat_client/design/gallery.dart';

import 'support/pump_app.dart';

Future<void> tap(WidgetTester tester, String label) async {
  await tester.ensureVisible(find.text(label).last);
  await tester.tap(find.text(label).last);
  await tester.pumpAndSettle();
}

/// Whether the screen showing is the placeholder for [template].
Finder screenFor(String template) =>
    find.textContaining('the design’s $template screen');

final signInAsOlivia = 'Sign in as ${SampleData.client.name}';

void main() {
  testWidgets('a signed-out client starts on Welcome and can browse', (
    tester,
  ) async {
    await pumpApp(tester);

    expect(screenFor('Main'), findsOneWidget);
    expect(
      find.text('API: http://localhost:8081 · data: fake'),
      findsOneWidget,
    );

    await tap(tester, 'Browse without an account');

    expect(screenFor('Home'), findsOneWidget);
    expect(find.byType(SalonatTabBar), findsOneWidget);
    // Favourites waits for phase 2.
    expect(find.text('Favourites'), findsNothing);
    expect(find.text('Profile'), findsOneWidget);
  });

  testWidgets('a signed-in client starts on Discover', (tester) async {
    await pumpApp(tester, signedIn: true);

    expect(screenFor('Home'), findsOneWidget);
  });

  testWidgets('every screen in the design has a location that shows it', (
    tester,
  ) async {
    final templates = Directory('design/templates')
        .listSync()
        .map((f) => f.uri.pathSegments.last)
        .where((name) => name.endsWith('.html'))
        .map((name) => name.replaceFirst('.html', ''))
        .where((name) => name != 'Foundations')
        .toSet();
    expect(designScreens.map((s) => s.$1).toSet(), templates);

    await pumpApp(tester, signedIn: true);
    for (final (template, location) in designScreens) {
      routerOf(tester).go(location);
      await tester.pumpAndSettle();
      // Signed in, Welcome and the sign-in screens send the client on to Discover.
      final shown = const ['Main', 'SignIn', 'SignUp'].contains(template)
          ? 'Home'
          : template;
      expect(screenFor(shown), findsOneWidget, reason: location);
    }
  });

  testWidgets('the sign-in screens are there for a signed-out client', (
    tester,
  ) async {
    await pumpApp(tester);

    for (final (template, location) in [
      ('SignIn', Routes.signIn),
      ('SignUp', Routes.signUp),
    ]) {
      routerOf(tester).go(location);
      await tester.pumpAndSettle();
      expect(screenFor(template), findsOneWidget);
    }
  });

  testWidgets('a tab that needs signing in asks for it, then opens', (
    tester,
  ) async {
    await pumpApp(tester);
    await tap(tester, 'Browse without an account');

    await tap(tester, 'Bookings');

    expect(screenFor('SignIn'), findsOneWidget);
    expect(locationOf(tester), Routes.signInFrom(Routes.bookings));

    await tap(tester, signInAsOlivia);

    expect(screenFor('Bookings'), findsOneWidget);
    expect(locationOf(tester), Routes.bookings);
  });

  testWidgets(
    'booking needs signing in only at its last step, then carries on',
    (tester) async {
      await pumpApp(tester);
      await tap(tester, 'Browse without an account');

      await tap(tester, SampleData.glow.name);
      expect(find.byType(SalonatTabBar), findsOneWidget);
      await tap(tester, 'Book ${SampleData.glow.services.first.name}');
      expect(screenFor('BookStaff'), findsOneWidget);
      // The booking flow covers the tab bar.
      expect(find.byType(SalonatTabBar), findsNothing);
      await tap(tester, 'Mia Stylist');
      expect(screenFor('BookTime'), findsOneWidget);
      await tap(tester, 'Continue');

      expect(screenFor('SignIn'), findsOneWidget);
      await tap(tester, signInAsOlivia);

      expect(screenFor('BookReview'), findsOneWidget);
      expect(locationOf(tester), Routes.bookReview(1, 1, staffId: 1));
      await tap(tester, 'Book');
      expect(screenFor('BookDone'), findsOneWidget);
      await tap(tester, 'See the booking');
      expect(screenFor('BookingDetail'), findsOneWidget);
    },
  );

  testWidgets('signing out goes back to Welcome and locks the tabs again', (
    tester,
  ) async {
    await pumpApp(tester, signedIn: true);

    await tap(tester, 'Profile');
    await tap(tester, 'Sign out');

    expect(screenFor('Main'), findsOneWidget);
    routerOf(tester).go(Routes.profile);
    await tester.pumpAndSettle();
    expect(screenFor('SignIn'), findsOneWidget);
  });

  testWidgets('each tab keeps its own place, until it is tapped again', (
    tester,
  ) async {
    await pumpApp(tester, signedIn: true);

    await tap(tester, SampleData.glow.name);
    expect(screenFor('Salon'), findsOneWidget);
    await tap(tester, 'Profile');
    expect(screenFor('Profile'), findsOneWidget);
    await tap(tester, 'Discover');
    expect(screenFor('Salon'), findsOneWidget);

    await tap(tester, 'Discover');
    expect(screenFor('Home'), findsOneWidget);
  });

  testWidgets('Back returns along the way the client came', (tester) async {
    await pumpApp(tester, signedIn: true);

    await tap(tester, 'Your next appointment');
    await tap(tester, 'Cancel appointment');
    expect(screenFor('CancelBooking'), findsOneWidget);

    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();

    expect(screenFor('BookingDetail'), findsOneWidget);
  });

  testWidgets('a debug build opens the design gallery from Welcome', (
    tester,
  ) async {
    await pumpApp(tester);

    await tap(tester, 'Design gallery');

    expect(find.byType(DesignGallery), findsOneWidget);
  });
}
