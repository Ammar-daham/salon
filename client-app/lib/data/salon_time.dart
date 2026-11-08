/// Times as the API sends them: on the salon's clock, without a time zone.
///
/// A [DateTime] read here holds the salon's wall-clock time in its fields. It's a UTC DateTime only
/// so the phone's own daylight saving can't move it: show its fields as they are, and never call
/// `toLocal()` on it.
library;

final _dateTime = RegExp(r'^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$');

/// Reads `yyyy-MM-ddTHH:mm`, such as `2026-11-06T10:15`.
DateTime parseSalonDateTime(String value) {
  final match = _dateTime.firstMatch(value);
  if (match == null) {
    throw FormatException('Not a yyyy-MM-ddTHH:mm time', value);
  }
  final [year, month, day, hour, minute] = [
    for (var i = 1; i <= 5; i++) int.parse(match.group(i)!),
  ];
  final time = DateTime.utc(year, month, day, hour, minute);
  // DateTime rolls a 13th month or a 25th hour over into the next, rather than refusing it.
  if (time.month != month ||
      time.day != day ||
      time.hour != hour ||
      time.minute != minute) {
    throw FormatException('Not a real time', value);
  }
  return time;
}

/// Writes `yyyy-MM-ddTHH:mm`.
String formatSalonDateTime(DateTime time) =>
    '${formatSalonDate(time)}T${_two(time.hour)}:${_two(time.minute)}';

/// Writes the day of [date] as `yyyy-MM-dd`, as the API takes dates.
String formatSalonDate(DateTime date) =>
    '${date.year.toString().padLeft(4, '0')}-${_two(date.month)}-${_two(date.day)}';

String _two(int n) => n.toString().padLeft(2, '0');
