import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/api/api_exception.dart';
import 'package:salonat_client/api/paging.dart';
import 'package:salonat_client/data/appointments/appointment.dart';
import 'package:salonat_client/data/appointments/appointment_repository.dart';
import 'package:salonat_client/data/appointments/fake_appointment_repository.dart';
import 'package:salonat_client/data/auth/fake_auth_repository.dart';
import 'package:salonat_client/data/availability/fake_availability_repository.dart';
import 'package:salonat_client/data/repositories.dart';
import 'package:salonat_client/data/salons/fake_salon_repository.dart';
import 'package:salonat_client/data/sample_data.dart';

Matcher failsWith(ApiErrorKind kind) =>
    throwsA(isA<ApiException>().having((e) => e.kind, 'kind', kind));

void main() {
  // A Sunday morning; Glow is open Tuesday to Saturday.
  DateTime now() => DateTime(2026, 11, 8, 9);

  group('FakeSalonRepository', () {
    final salons = FakeSalonRepository();

    test('searches names, descriptions and cities', () async {
      Future<List<String>> names(String q) async =>
          (await salons.listSalons(PageQuery(q: q))).items
              .map((s) => s.name)
              .toList();

      expect(await names('GLOW'), ['Glow Beauty Studio']);
      expect(await names('fades'), ['Urban Cuts Barbershop']);
      expect(await names('berlin'), hasLength(2));
      expect(await names('paris'), isEmpty);
    });

    test('pages and sorts the way the API does', () async {
      final first = await salons.listSalons(
        const PageQuery(size: 1, sort: '-name'),
      );
      final second = await salons.listSalons(
        const PageQuery(page: 2, size: 1, sort: '-name'),
      );

      expect(first.items.single.name, 'Urban Cuts Barbershop');
      expect(first.totalPages, 2);
      expect(first.hasMore, isTrue);
      expect(second.items.single.name, 'Glow Beauty Studio');
      expect(second.hasMore, isFalse);
      await expectLater(
        salons.listSalons(const PageQuery(size: 101)),
        failsWith(ApiErrorKind.validation),
      );
    });

    test('a salon that isn’t there is not found', () async {
      await expectLater(salons.getSalon(99), failsWith(ApiErrorKind.notFound));
    });
  });

  group('FakeAvailabilityRepository', () {
    final availability = FakeAvailabilityRepository(now: now);

    test(
      'offers quarter hours inside opening hours, the service long',
      () async {
        // Tuesday: 09:00 to 18:00 at Glow; a haircut takes 45 minutes.
        final tuesday = await availability.getAvailability(
          salonId: 1,
          serviceId: 1,
          from: DateTime(2026, 11, 10),
        );

        final mia = tuesday.staff.first.slots;
        expect(mia.first.startsAt, DateTime.utc(2026, 11, 10, 9));
        expect(mia.first.endsAt, DateTime.utc(2026, 11, 10, 9, 45));
        expect(mia[1].startsAt, DateTime.utc(2026, 11, 10, 9, 15));
        expect(mia.last.startsAt, DateTime.utc(2026, 11, 10, 17, 15));
        final noah = tuesday.staff.last.slots;
        expect(noah.every((s) => s.startsAt.minute % 30 == 0), isTrue);
      },
    );

    test('a closed day, or one already past, has no slots', () async {
      final sunday = await availability.getAvailability(
        salonId: 1,
        serviceId: 1,
        from: DateTime(2026, 11, 8),
      );
      final lastWeek = await availability.getAvailability(
        salonId: 1,
        serviceId: 1,
        from: DateTime(2026, 11, 3),
      );

      expect(sunday.anyoneFree, isEmpty);
      expect(lastWeek.anyoneFree, isEmpty);
    });

    test('refuses what the API refuses', () async {
      await expectLater(
        availability.getAvailability(
          salonId: 1,
          serviceId: 4,
          from: DateTime(2026, 11, 10),
        ),
        failsWith(ApiErrorKind.notFound),
      );
      await expectLater(
        availability.getAvailability(
          salonId: 1,
          serviceId: 1,
          from: DateTime(2026, 11, 10),
          to: DateTime(2026, 12, 20),
        ),
        failsWith(ApiErrorKind.validation),
      );
    });
  });

  group('FakeAppointmentRepository', () {
    late FakeAppointmentRepository appointments;
    setUp(() => appointments = FakeAppointmentRepository(now: now));

    final tuesdayAt10 = DateTime.utc(2026, 11, 10, 10);

    test('starts with one upcoming and one past appointment', () async {
      final upcoming = await appointments.listMine(AppointmentPeriod.upcoming);
      final past = await appointments.listMine(AppointmentPeriod.past);

      expect(upcoming.single.status, AppointmentStatus.confirmed);
      expect(upcoming.single.startsAt, DateTime.utc(2026, 11, 11, 10, 15));
      expect(past.single.status, AppointmentStatus.completed);
    });

    test('books with anyone free, awaiting confirmation', () async {
      final first = await appointments.book(
        BookingRequest(salonId: 1, serviceId: 1, startsAt: tuesdayAt10),
      );
      final second = await appointments.book(
        BookingRequest(salonId: 1, serviceId: 1, startsAt: tuesdayAt10),
      );

      expect(first.status, AppointmentStatus.booked);
      expect(first.endsAt, DateTime.utc(2026, 11, 10, 10, 45));
      expect(first.price, 45);
      // Mia is taken, so Noah.
      expect(
        [first.staff.name, second.staff.name],
        ['Mia Stylist', 'Noah Colorist'],
      );
      expect(await appointments.getMine(first.id), same(first));
    });

    test('a time someone took first is a conflict', () async {
      final request = BookingRequest(
        salonId: 1,
        serviceId: 1,
        staffId: 1,
        startsAt: tuesdayAt10,
      );
      await appointments.book(request);

      await expectLater(
        appointments.book(request),
        failsWith(ApiErrorKind.conflict),
      );
    });

    test('cancels before the notice runs out, and not after', () async {
      final soon = await appointments.book(
        BookingRequest(
          salonId: 1,
          serviceId: 1,
          startsAt: DateTime.utc(2026, 11, 8, 20),
        ),
      );
      final later = await appointments.book(
        BookingRequest(salonId: 1, serviceId: 1, startsAt: tuesdayAt10),
      );

      final cancelled = await appointments.cancel(later.id, reason: 'Ill');

      expect(cancelled.status, AppointmentStatus.cancelled);
      expect(
        (await appointments.listMine(AppointmentPeriod.past)).first.id,
        later.id,
      );
      await expectLater(
        appointments.cancel(later.id),
        failsWith(ApiErrorKind.conflict),
      );
      await expectLater(
        appointments.cancel(soon.id),
        failsWith(ApiErrorKind.validation),
      );
    });
  });

  group('FakeAuthRepository', () {
    test('signs in with the sample client, and out again', () async {
      final auth = FakeAuthRepository();

      expect(await auth.currentUser(), isNull);
      final user = await auth.signIn(
        email: ' Olivia@Example.com ',
        password: SampleData.clientPassword,
      );
      expect(user.name, 'Olivia Weber');
      expect(await auth.currentUser(), same(user));

      await auth.signOut();
      expect(await auth.currentUser(), isNull);
    });

    test('a wrong password is refused, and too many lock sign-in', () async {
      final auth = FakeAuthRepository();

      for (var i = 0; i < FakeAuthRepository.maxFailedTries; i++) {
        await expectLater(
          auth.signIn(email: 'olivia@example.com', password: 'nope'),
          failsWith(ApiErrorKind.unauthorized),
        );
      }
      await expectLater(
        auth.signIn(
          email: 'olivia@example.com',
          password: SampleData.clientPassword,
        ),
        failsWith(ApiErrorKind.tooManyRequests),
      );
    });

    test('can start signed in', () async {
      expect(await FakeAuthRepository(signedIn: true).currentUser(), isNotNull);
    });
  });

  test('fakes answer after the latency given', () async {
    final repositories = Repositories.fake(
      latency: const Duration(milliseconds: 30),
    );
    final watch = Stopwatch()..start();

    await repositories.salons.getSalon(1);

    expect(watch.elapsedMilliseconds, greaterThanOrEqualTo(30));
  });
}
