import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:salonat_client/app.dart';
import 'package:salonat_client/app/providers.dart';
import 'package:salonat_client/app/router.dart';
import 'package:salonat_client/config.dart';
import 'package:salonat_client/data/appointments/fake_appointment_repository.dart';
import 'package:salonat_client/data/auth/fake_auth_repository.dart';
import 'package:salonat_client/data/availability/fake_availability_repository.dart';
import 'package:salonat_client/data/hours/fake_opening_hours_repository.dart';
import 'package:salonat_client/data/repositories.dart';
import 'package:salonat_client/data/salons/fake_salon_repository.dart';
import 'package:salonat_client/data/staff/fake_staff_repository.dart';

/// Starts the whole app on the fakes, signed in or not, and waits for its first screen.
Future<void> pumpApp(WidgetTester tester, {bool signedIn = false}) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        configProvider.overrideWithValue(
          const AppConfig(apiBaseUrl: 'http://localhost:8081'),
        ),
        repositoriesProvider.overrideWithValue(
          Repositories(
            auth: FakeAuthRepository(signedIn: signedIn),
            salons: FakeSalonRepository(),
            openingHours: FakeOpeningHoursRepository(),
            staff: FakeStaffRepository(),
            availability: FakeAvailabilityRepository(),
            appointments: FakeAppointmentRepository(),
          ),
        ),
      ],
      child: const SalonatApp(),
    ),
  );
  await tester.pumpAndSettle();
}

/// The app's router, for going straight to a location.
GoRouter routerOf(WidgetTester tester) =>
    ProviderScope.containerOf(tester.element(find.byType(SalonatApp)))
        .read(routerProvider);

/// Where the app is now.
String locationOf(WidgetTester tester) =>
    routerOf(tester).routerDelegate.currentConfiguration.uri.toString();
