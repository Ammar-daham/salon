import '../../api/api_client.dart';
import '../../api/endpoints.dart';
import 'opening_hours.dart';

/// When salons are open.
abstract interface class OpeningHoursRepository {
  Future<OpeningHours> getOpeningHours(int salonId);
}

class ApiOpeningHoursRepository implements OpeningHoursRepository {
  const ApiOpeningHoursRepository(this._api);

  final ApiClient _api;

  @override
  Future<OpeningHours> getOpeningHours(int salonId) => _api.get(
    Endpoints.openingHours(salonId),
    (body) => OpeningHours.fromJson(asJson(body)),
  );
}
