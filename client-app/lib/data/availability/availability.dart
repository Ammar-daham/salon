import '../../api/api_client.dart';
import '../salon_time.dart';

/// When one service can be booked, per staff member who performs it.
class Availability {
  const Availability({
    required this.timezone,
    required this.durationMinutes,
    required this.staff,
  });

  factory Availability.fromJson(Json json) => Availability(
    timezone: json['timezone']! as String,
    durationMinutes: json['duration_minutes']! as int,
    staff: asList(json['staff'], StaffSlots.fromJson),
  );

  /// The salon's IANA time zone: the clock the slots are on.
  final String timezone;

  /// How long the service takes, and so each slot.
  final int durationMinutes;

  /// Each active staff member who performs the service, even one with no free slots.
  final List<StaffSlots> staff;

  /// Every start time anyone is free at, in order and each once: for booking with anyone.
  List<DateTime> get anyoneFree {
    final starts = {
      for (final member in staff)
        for (final slot in member.slots) slot.startsAt,
    }.toList()..sort();
    return starts;
  }
}

/// One staff member's free slots.
class StaffSlots {
  const StaffSlots({
    required this.staffId,
    required this.firstName,
    required this.lastName,
    required this.slots,
  });

  factory StaffSlots.fromJson(Json json) => StaffSlots(
    staffId: json['id']! as int,
    firstName: json['first_name']! as String,
    lastName: json['last_name']! as String,
    slots: asList(json['slots'], Slot.fromJson),
  );

  final int staffId;
  final String firstName;
  final String lastName;
  final List<Slot> slots;
}

/// A time the service could be booked for, on the salon's clock (see `salon_time.dart`).
class Slot {
  const Slot({required this.startsAt, required this.endsAt});

  factory Slot.fromJson(Json json) => Slot(
    startsAt: parseSalonDateTime(json['starts_at']! as String),
    endsAt: parseSalonDateTime(json['ends_at']! as String),
  );

  final DateTime startsAt;
  final DateTime endsAt;
}
