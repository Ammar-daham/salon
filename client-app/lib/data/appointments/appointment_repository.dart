import 'appointment.dart';

/// Which of the client's appointments to list.
enum AppointmentPeriod {
  /// Still to come, soonest first.
  upcoming,

  /// Over, cancelled or missed, latest first.
  past,
}

/// What the client asks to book.
class BookingRequest {
  const BookingRequest({
    required this.salonId,
    required this.serviceId,
    required this.startsAt,
    this.staffId,
    this.notes,
  });

  final int salonId;
  final int serviceId;

  /// Who with, or null for anyone free at [startsAt].
  final int? staffId;

  /// On the salon's clock, one of the times availability offers.
  final DateTime startsAt;

  /// A note for the salon.
  final String? notes;
}

/// The signed-in client's own appointments, across every salon.
///
/// Only `FakeAppointmentRepository` for now: salon-backend gets the client's own booking, list and
/// cancel endpoints in tasks B6 to B8 of the plan.
abstract interface class AppointmentRepository {
  Future<List<Appointment>> listMine(AppointmentPeriod period);

  Future<Appointment> getMine(int id);

  /// Books as [AppointmentStatus.booked], awaiting the salon's confirmation. Fails with a conflict
  /// when someone else took the time first.
  Future<Appointment> book(BookingRequest request);

  /// Fails once the salon's cancellation notice has passed, or when it's already over.
  Future<Appointment> cancel(int id, {String? reason});
}
