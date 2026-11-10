import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/api/api_exception.dart';
import 'package:salonat_client/api/paging.dart';
import 'package:salonat_client/data/availability/availability_repository.dart';
import 'package:salonat_client/data/hours/opening_hours_repository.dart';
import 'package:salonat_client/data/salons/salon_repository.dart';
import 'package:salonat_client/data/staff/staff_repository.dart';

import '../support/fake_http.dart';
import 'api_json.dart';

void main() {
  group('ApiSalonRepository', () {
    test('asks for a page of salons and reads it', () async {
      final http = FakeHttp.always(200, {
        'items': [glowJson],
        'page': 2,
        'size': 1,
        'total_items': 3,
        'total_pages': 3,
      });

      final page = await ApiSalonRepository(http.client)
          .listSalons(const PageQuery(page: 2, size: 1, q: 'berlin'));

      final request = http.requests.single;
      expect(request.path, '/api/v1/businesses');
      expect(request.queryParameters, {'q': 'berlin', 'page': 2, 'size': 1});
      expect(page.items.single.name, 'Glow Beauty Studio');
      expect(page.totalItems, 3);
      expect(page.hasMore, isTrue);
    });

    test('a salon that isn’t there is not found', () async {
      final http = FakeHttp.always(404, {
        'errorCode': '404',
        'message': 'Business not found',
        'status': 'NOT_FOUND',
        'timestamp': '2026-11-08T10:00:00Z',
        'path': '/api/v1/businesses/9',
      });

      await expectLater(
        ApiSalonRepository(http.client).getSalon(9),
        throwsA(
          isA<ApiException>().having(
            (e) => e.kind,
            'kind',
            ApiErrorKind.notFound,
          ),
        ),
      );
      expect(http.requests.single.path, '/api/v1/businesses/9');
    });

    test('a page without its items is a bad response', () async {
      final http = FakeHttp.always(200, {'page': 1});

      await expectLater(
        ApiSalonRepository(http.client).listSalons(const PageQuery()),
        throwsA(
          isA<ApiException>().having(
            (e) => e.kind,
            'kind',
            ApiErrorKind.badResponse,
          ),
        ),
      );
    });
  });

  test('ApiOpeningHoursRepository reads a salon’s week', () async {
    final http = FakeHttp.always(200, urbanHoursJson);

    final hours = await ApiOpeningHoursRepository(http.client)
        .getOpeningHours(2);

    expect(http.requests.single.path, '/api/v1/businesses/2/hours');
    expect(hours.intervals, hasLength(3));
  });

  test('ApiStaffRepository reads a salon’s team', () async {
    final http = FakeHttp.always(200, [miaJson]);

    final team = await ApiStaffRepository(http.client).listStaff(1);

    expect(http.requests.single.path, '/api/v1/businesses/1/staff');
    expect(team.single.name, 'Mia Stylist');
  });

  group('ApiAvailabilityRepository', () {
    test('asks for one day when given one', () async {
      final http = FakeHttp.always(200, availabilityJson);

      final availability = await ApiAvailabilityRepository(http.client)
          .getAvailability(
            salonId: 1,
            serviceId: 1,
            from: DateTime.utc(2026, 11, 10),
          );

      final request = http.requests.single;
      expect(request.path, '/api/v1/businesses/1/services/1/availability');
      expect(request.queryParameters, {
        'from': '2026-11-10',
        'to': '2026-11-10',
      });
      expect(availability.staff, hasLength(2));
    });

    test('asks for a range, for one staff member', () async {
      final http = FakeHttp.always(200, availabilityJson);

      await ApiAvailabilityRepository(http.client).getAvailability(
        salonId: 1,
        serviceId: 1,
        from: DateTime(2026, 11, 10, 23, 30),
        to: DateTime(2026, 11, 16),
        staffId: 2,
      );

      expect(http.requests.single.queryParameters, {
        'from': '2026-11-10',
        'to': '2026-11-16',
        'staff_id': 2,
      });
    });
  });
}
