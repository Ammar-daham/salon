import '../../api/api_client.dart';
import '../../api/endpoints.dart';
import '../salon_time.dart';
import 'availability.dart';

/// When services can be booked.
abstract interface class AvailabilityRepository {
  /// The free slots for a service from [from] to [to], both days included and at most 31 days
  /// apart; [to] is the same day as [from] when left out. With [staffId], only that staff member's.
  ///
  /// The days are on the salon's clock, and slots already past are left out.
  Future<Availability> getAvailability({
    required int salonId,
    required int serviceId,
    required DateTime from,
    DateTime? to,
    int? staffId,
  });
}

class ApiAvailabilityRepository implements AvailabilityRepository {
  const ApiAvailabilityRepository(this._api);

  final ApiClient _api;

  @override
  Future<Availability> getAvailability({
    required int salonId,
    required int serviceId,
    required DateTime from,
    DateTime? to,
    int? staffId,
  }) => _api.get(
    Endpoints.availability(salonId, serviceId),
    (body) => Availability.fromJson(asJson(body)),
    query: {
      'from': formatSalonDate(from),
      'to': formatSalonDate(to ?? from),
      'staff_id': ?staffId,
    },
  );
}
