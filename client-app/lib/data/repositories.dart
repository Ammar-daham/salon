import '../api/api_client.dart';
import 'appointments/appointment_repository.dart';
import 'appointments/fake_appointment_repository.dart';
import 'auth/auth_repository.dart';
import 'auth/fake_auth_repository.dart';
import 'availability/availability_repository.dart';
import 'availability/fake_availability_repository.dart';
import 'hours/fake_opening_hours_repository.dart';
import 'hours/opening_hours_repository.dart';
import 'salons/fake_salon_repository.dart';
import 'salons/salon_repository.dart';
import 'staff/fake_staff_repository.dart';
import 'staff/staff_repository.dart';

/// Where the app's data comes from, one repository per resource.
class Repositories {
  const Repositories({
    required this.auth,
    required this.salons,
    required this.openingHours,
    required this.staff,
    required this.availability,
    required this.appointments,
  });

  /// From salon-backend, apart from what it can't serve the app yet: signing in and the client's
  /// own appointments, which stay fakes until tasks B2 and B6 to B8.
  factory Repositories.api(ApiClient api) => Repositories(
    auth: FakeAuthRepository(),
    salons: ApiSalonRepository(api),
    openingHours: ApiOpeningHoursRepository(api),
    staff: ApiStaffRepository(api),
    availability: ApiAvailabilityRepository(api),
    appointments: FakeAppointmentRepository(),
  );

  /// All fakes, answering after [latency]: for building screens without the API, and for tests.
  factory Repositories.fake({Duration latency = Duration.zero}) => Repositories(
    auth: FakeAuthRepository(latency: latency),
    salons: FakeSalonRepository(latency: latency),
    openingHours: FakeOpeningHoursRepository(latency: latency),
    staff: FakeStaffRepository(latency: latency),
    availability: FakeAvailabilityRepository(latency: latency),
    appointments: FakeAppointmentRepository(latency: latency),
  );

  final AuthRepository auth;
  final SalonRepository salons;
  final OpeningHoursRepository openingHours;
  final StaffRepository staff;
  final AvailabilityRepository availability;
  final AppointmentRepository appointments;
}
