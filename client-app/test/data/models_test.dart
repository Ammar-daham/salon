import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/data/appointments/appointment.dart';
import 'package:salonat_client/data/auth/auth_user.dart';
import 'package:salonat_client/data/availability/availability.dart';
import 'package:salonat_client/data/hours/opening_hours.dart';
import 'package:salonat_client/data/salons/salon.dart';
import 'package:salonat_client/data/staff/staff_member.dart';

import 'api_json.dart';

void main() {
  group('Salon', () {
    test('reads the API’s salon, with its address, contacts and services', () {
      final salon = Salon.fromJson(glowJson);

      expect(salon.id, 1);
      expect(salon.name, 'Glow Beauty Studio');
      expect(salon.currency, 'EUR');
      expect(salon.timezone, 'Europe/Berlin');
      expect(salon.address!.street, '12 Rosenthaler Str.');
      expect(salon.address!.postalCode, '10119');
      expect(salon.address!.latitude, 52.53);
      expect(salon.phone, '+49 30 1234501');
      expect(salon.services.first.name, 'Signature Haircut');
      expect(salon.services.first.durationMinutes, 45);
      // 45.00 arrives as a whole number.
      expect(salon.services.first.price, 45.0);
      expect(salon.services.last.price, 150.5);
    });

    test('reads missing lists and details as none', () {
      final salon = Salon.fromJson({
        ...glowJson,
        'description': null,
        'addresses': null,
        'contacts': null,
        'services': null,
      });

      expect(salon.description, isNull);
      expect(salon.address, isNull);
      expect(salon.phone, isNull);
      expect(salon.services, isEmpty);
    });

    test('refuses a salon without a name', () {
      expect(
        () => Salon.fromJson({...glowJson, 'name': null}),
        throwsA(isA<TypeError>()),
      );
    });
  });

  test('OpeningHours reads the week, and the hours of one day', () {
    final hours = OpeningHours.fromJson(urbanHoursJson);

    expect(hours.timezone, 'Europe/Berlin');
    final monday = hours.on(DayOfWeek.monday);
    expect(monday.map((i) => '${i.opensAt}-${i.closesAt}'), [
      '10:00-13:00',
      '14:00-20:00',
    ]);
    expect(hours.on(DayOfWeek.sunday), isEmpty);
  });

  test('DayOfWeek refuses a name it doesn’t know, and finds a date’s day', () {
    expect(() => DayOfWeek.fromApi('FUNDAY'), throwsFormatException);
    expect(DayOfWeek.of(DateTime.utc(2026, 11, 8)), DayOfWeek.sunday);
  });

  test('StaffMember reads the API’s staff record', () {
    final member = StaffMember.fromJson(miaJson);

    expect(member.id, 1);
    expect(member.name, 'Mia Stylist');
    expect(member.title, 'Senior Stylist');
    expect(member.isActive, isTrue);
  });

  test('Availability reads each staff member’s slots on the salon’s clock', () {
    final availability = Availability.fromJson(availabilityJson);

    expect(availability.durationMinutes, 45);
    expect(availability.staff.map((s) => s.firstName), ['Mia', 'Noah']);
    final slot = availability.staff.first.slots.first;
    expect(slot.startsAt, DateTime.utc(2026, 11, 10, 9));
    expect(slot.endsAt, DateTime.utc(2026, 11, 10, 9, 45));
    expect(availability.staff.last.slots, isEmpty);
    expect(availability.anyoneFree, [
      DateTime.utc(2026, 11, 10, 9),
      DateTime.utc(2026, 11, 10, 9, 15),
    ]);
  });

  group('Appointment', () {
    const salon = SalonRef(
      id: 1,
      name: 'Glow Beauty Studio',
      currency: 'EUR',
      timezone: 'Europe/Berlin',
    );

    test('reads the API’s appointment, with the salon it was asked at', () {
      final appointment = Appointment.fromJson(appointmentJson, salon: salon);

      expect(appointment.id, 7);
      expect(appointment.salon.name, 'Glow Beauty Studio');
      expect(appointment.staff.name, 'Mia Stylist');
      expect(appointment.serviceName, 'Signature Haircut');
      expect(appointment.startsAt, DateTime.utc(2026, 11, 10, 10, 15));
      expect(appointment.status, AppointmentStatus.booked);
      expect(appointment.price, 45.0);
      expect(appointment.notes, 'Window seat, please');
    });

    test('knows every status, and refuses one it doesn’t', () {
      expect(AppointmentStatus.values.map((s) => s.apiValue), [
        'BOOKED',
        'CONFIRMED',
        'COMPLETED',
        'CANCELLED',
        'NO_SHOW',
      ]);
      expect(AppointmentStatus.fromApi('NO_SHOW'), AppointmentStatus.noShow);
      expect(
        () => Appointment.fromJson({
          ...appointmentJson,
          'status': 'LOST',
        }, salon: salon),
        throwsFormatException,
      );
    });

    test('only booked and confirmed appointments are still open', () {
      expect(AppointmentStatus.values.where((s) => s.isOpen), [
        AppointmentStatus.booked,
        AppointmentStatus.confirmed,
      ]);
    });
  });

  test('AuthUser reads who is signed in', () {
    final user = AuthUser.fromJson({
      'id': 10,
      'first_name': 'Olivia',
      'last_name': 'Weber',
      'email': 'olivia@example.com',
      'role': 'CUSTOMER',
      'business_id': null,
    });

    expect(user.name, 'Olivia Weber');
    expect(user.role, Role.customer);
    expect(() => Role.fromApi('OWNER'), throwsFormatException);
  });
}
