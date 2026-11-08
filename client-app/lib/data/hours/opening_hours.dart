import '../../api/api_client.dart';

/// The ISO days of the week, as the API names them.
enum DayOfWeek {
  monday('MONDAY'),
  tuesday('TUESDAY'),
  wednesday('WEDNESDAY'),
  thursday('THURSDAY'),
  friday('FRIDAY'),
  saturday('SATURDAY'),
  sunday('SUNDAY');

  const DayOfWeek(this.apiValue);

  final String apiValue;

  static DayOfWeek fromApi(String value) => values.firstWhere(
    (day) => day.apiValue == value,
    orElse: () => throw FormatException('Not a day of the week', value),
  );

  /// The day [date] falls on.
  static DayOfWeek of(DateTime date) => values[date.weekday - 1];
}

/// One stretch of time a salon is open, on one day.
class OpeningInterval {
  const OpeningInterval({
    required this.day,
    required this.opensAt,
    required this.closesAt,
  });

  factory OpeningInterval.fromJson(Json json) => OpeningInterval(
    day: DayOfWeek.fromApi(json['day_of_week']! as String),
    opensAt: json['opens_at']! as String,
    closesAt: json['closes_at']! as String,
  );

  final DayOfWeek day;

  /// `HH:mm` on the salon's clock.
  final String opensAt;

  /// `HH:mm` on the salon's clock, later the same day.
  final String closesAt;
}

/// A salon's week. A day with no interval is closed, and two on one day leave a break between.
class OpeningHours {
  const OpeningHours({required this.timezone, required this.intervals});

  factory OpeningHours.fromJson(Json json) => OpeningHours(
    timezone: json['timezone']! as String,
    intervals: asList(json['hours'], OpeningInterval.fromJson),
  );

  /// The salon's IANA time zone: the clock the times are on.
  final String timezone;

  /// In day order, then time order.
  final List<OpeningInterval> intervals;

  /// The stretches the salon is open on [day], none if it's closed.
  List<OpeningInterval> on(DayOfWeek day) =>
      intervals.where((i) => i.day == day).toList();
}
