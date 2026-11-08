import '../fake_call.dart';
import '../hours/opening_hours.dart';
import '../sample_data.dart';
import 'availability.dart';
import 'availability_repository.dart';

/// [AvailabilityRepository] without the API. Every sample staff member performs every service at
/// their salon and works its opening hours; the first is free on every quarter hour and the others
/// on every half hour, so the two lists differ.
class FakeAvailabilityRepository implements AvailabilityRepository {
  FakeAvailabilityRepository({
    this.latency = Duration.zero,
    DateTime Function()? now,
  }) : _now = now ?? DateTime.now;

  final Duration latency;

  /// The time on the salon's clock, before which there are no slots.
  final DateTime Function() _now;

  @override
  Future<Availability> getAvailability({
    required int salonId,
    required int serviceId,
    required DateTime from,
    DateTime? to,
    int? staffId,
  }) => fakeCall(latency, () {
    final salon = SampleData.salons.where((s) => s.id == salonId).firstOrNull;
    final service = salon?.services.where((s) => s.id == serviceId).firstOrNull;
    if (salon == null || service == null) {
      throw fakeError(404);
    }
    final first = _day(from);
    final last = _day(to ?? from);
    if (last.isBefore(first) || last.difference(first).inDays > 30) {
      throw fakeError(400);
    }
    final team = SampleData.staff[salonId]!;
    if (staffId != null && !team.any((member) => member.id == staffId)) {
      throw fakeError(400);
    }

    final hours = SampleData.hours[salonId]!;
    final length = Duration(minutes: service.durationMinutes);
    final n = _now();
    final now = DateTime.utc(n.year, n.month, n.day, n.hour, n.minute);
    return Availability(
      timezone: salon.timezone,
      durationMinutes: service.durationMinutes,
      staff: [
        for (final (i, member) in team.indexed)
          if (staffId == null || member.id == staffId)
            StaffSlots(
              staffId: member.id,
              firstName: member.firstName,
              lastName: member.lastName,
              slots: [
                for (
                  var day = first;
                  !day.isAfter(last);
                  day = day.add(const Duration(days: 1))
                )
                  for (final open in hours.on(DayOfWeek.of(day)))
                    for (
                      var start = _at(day, open.opensAt);
                      !start.add(length).isAfter(_at(day, open.closesAt));
                      start = start.add(const Duration(minutes: 15))
                    )
                      if (start.isAfter(now) &&
                          (i == 0 || start.minute % 30 == 0))
                        Slot(startsAt: start, endsAt: start.add(length)),
              ],
            ),
      ],
    );
  });

  static DateTime _day(DateTime date) =>
      DateTime.utc(date.year, date.month, date.day);

  /// [day] at [time], `HH:mm`.
  static DateTime _at(DateTime day, String time) {
    final [hour, minute] = time.split(':').map(int.parse).toList();
    return day.add(Duration(hours: hour, minutes: minute));
  }
}
