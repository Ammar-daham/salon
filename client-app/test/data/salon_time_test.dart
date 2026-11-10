import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/data/salon_time.dart';

void main() {
  test('reads a time on the salon’s clock into the same fields', () {
    final time = parseSalonDateTime('2026-11-06T10:15');

    expect(
      [time.year, time.month, time.day, time.hour, time.minute],
      [2026, 11, 6, 10, 15],
    );
  });

  test('keeps a time the phone’s daylight saving skips', () {
    // 02:30 doesn't exist in Berlin on this day, but can on a salon's clock elsewhere.
    final time = parseSalonDateTime('2027-03-28T02:30');

    expect(time.hour, 2);
    expect(time.minute, 30);
  });

  test('refuses anything that isn’t yyyy-MM-ddTHH:mm', () {
    for (final value in [
      '2026-11-06',
      '2026-11-06T10:15:00',
      '2026-11-06 10:15',
      '2026-13-06T10:15',
      '2026-02-30T10:15',
      '2026-11-06T24:00',
    ]) {
      expect(
        () => parseSalonDateTime(value),
        throwsFormatException,
        reason: value,
      );
    }
  });

  test('writes times and dates back as the API takes them', () {
    final time = DateTime.utc(2026, 3, 4, 9, 5);

    expect(formatSalonDateTime(time), '2026-03-04T09:05');
    expect(formatSalonDate(time), '2026-03-04');
  });
}
