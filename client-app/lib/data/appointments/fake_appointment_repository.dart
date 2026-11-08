import '../fake_call.dart';
import '../sample_data.dart';
import 'appointment.dart';
import 'appointment_repository.dart';

/// [AppointmentRepository] without the API, keeping the client's appointments in memory. It starts
/// with one confirmed appointment three days ahead and one completed two weeks ago.
class FakeAppointmentRepository implements AppointmentRepository {
  FakeAppointmentRepository({
    this.latency = Duration.zero,
    this.cancellationNotice = const Duration(hours: 24),
    DateTime Function()? now,
    List<Appointment>? appointments,
  }) : _now = now ?? DateTime.now {
    _appointments = appointments ?? _samples(_salonNow());
  }

  final Duration latency;

  /// How long before an appointment it can still be cancelled.
  final Duration cancellationNotice;

  final DateTime Function() _now;
  late final List<Appointment> _appointments;

  @override
  Future<List<Appointment>> listMine(AppointmentPeriod period) =>
      fakeCall(latency, () {
        final now = _salonNow();
        bool upcoming(Appointment a) =>
            a.status.isOpen && a.startsAt.isAfter(now);
        return switch (period) {
          AppointmentPeriod.upcoming =>
            _appointments.where(upcoming).toList()
              ..sort((a, b) => a.startsAt.compareTo(b.startsAt)),
          AppointmentPeriod.past =>
            _appointments.where((a) => !upcoming(a)).toList()
              ..sort((a, b) => b.startsAt.compareTo(a.startsAt)),
        };
      });

  @override
  Future<Appointment> getMine(int id) => fakeCall(latency, () => _find(id));

  @override
  Future<Appointment> book(BookingRequest request) => fakeCall(latency, () {
    final salon = SampleData.salons
        .where((s) => s.id == request.salonId)
        .firstOrNull;
    final service = salon?.services
        .where((s) => s.id == request.serviceId)
        .firstOrNull;
    if (salon == null || service == null) {
      throw fakeError(404);
    }
    if (!request.startsAt.isAfter(_salonNow())) {
      throw fakeError(400, 'That time has passed. Please pick another.');
    }
    final start = request.startsAt;
    final end = start.add(Duration(minutes: service.durationMinutes));
    bool free(int staffId) => !_appointments.any(
      (a) =>
          a.staff.id == staffId &&
          a.status.isOpen &&
          a.startsAt.isBefore(end) &&
          start.isBefore(a.endsAt),
    );
    final team = SampleData.staff[salon.id]!;
    final member = team
        .where(
          (m) =>
              (request.staffId == null || m.id == request.staffId) &&
              free(m.id),
        )
        .firstOrNull;
    if (member == null) {
      throw fakeError(
        409,
        'Someone just booked that time. Please pick another.',
      );
    }

    final appointment = Appointment(
      id: _appointments.fold(0, (max, a) => a.id > max ? a.id : max) + 1,
      salon: SalonRef.of(salon),
      staff: PersonRef(
        id: member.id,
        firstName: member.firstName,
        lastName: member.lastName,
      ),
      serviceId: service.id,
      serviceName: service.name,
      startsAt: start,
      endsAt: end,
      status: AppointmentStatus.booked,
      price: service.price,
      notes: request.notes?.trim().isEmpty ?? true
          ? null
          : request.notes!.trim(),
    );
    _appointments.add(appointment);
    return appointment;
  });

  /// The reason isn't kept, since the app doesn't show it back.
  @override
  Future<Appointment> cancel(int id, {String? reason}) => fakeCall(latency, () {
    final appointment = _find(id);
    if (!appointment.status.isOpen) {
      throw fakeError(409, 'This appointment can’t be cancelled any more.');
    }
    if (appointment.startsAt.difference(_salonNow()) < cancellationNotice) {
      throw fakeError(
        400,
        'It’s too late to cancel in the app. Please call the salon.',
      );
    }
    final cancelled = appointment.copyWith(status: AppointmentStatus.cancelled);
    _appointments[_appointments.indexOf(appointment)] = cancelled;
    return cancelled;
  });

  Appointment _find(int id) => _appointments.firstWhere(
    (a) => a.id == id,
    orElse: () => throw fakeError(404),
  );

  /// Now, held like a time on the salon's clock (see `salon_time.dart`).
  DateTime _salonNow() {
    final n = _now();
    return DateTime.utc(n.year, n.month, n.day, n.hour, n.minute);
  }

  static List<Appointment> _samples(DateTime now) {
    final today = DateTime.utc(now.year, now.month, now.day);
    Appointment sample({
      required int id,
      required DateTime startsAt,
      required int serviceIndex,
      required int salonIndex,
      required AppointmentStatus status,
    }) {
      final salon = SampleData.salons[salonIndex];
      final service = salon.services[serviceIndex];
      final member = SampleData.staff[salon.id]!.first;
      return Appointment(
        id: id,
        salon: SalonRef.of(salon),
        staff: PersonRef(
          id: member.id,
          firstName: member.firstName,
          lastName: member.lastName,
        ),
        serviceId: service.id,
        serviceName: service.name,
        startsAt: startsAt,
        endsAt: startsAt.add(Duration(minutes: service.durationMinutes)),
        status: status,
        price: service.price,
      );
    }

    return [
      sample(
        id: 1,
        startsAt: today.add(const Duration(days: 3, hours: 10, minutes: 15)),
        salonIndex: 0,
        serviceIndex: 0,
        status: AppointmentStatus.confirmed,
      ),
      sample(
        id: 2,
        startsAt: today
            .subtract(const Duration(days: 14))
            .add(const Duration(hours: 17)),
        salonIndex: 1,
        serviceIndex: 1,
        status: AppointmentStatus.completed,
      ),
    ];
  }
}
