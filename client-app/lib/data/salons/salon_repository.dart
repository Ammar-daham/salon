import '../../api/api_client.dart';
import '../../api/endpoints.dart';
import '../../api/paging.dart';
import 'salon.dart';

/// The salons a client can find and book.
abstract interface class SalonRepository {
  /// A page of salons. [PageQuery.q] matches a salon's name, description or city, and
  /// [PageQuery.sort] is `name` (the default) or `created_at`.
  Future<Page<Salon>> listSalons(PageQuery query);

  /// One salon, with its services.
  Future<Salon> getSalon(int id);
}

class ApiSalonRepository implements SalonRepository {
  const ApiSalonRepository(this._api);

  final ApiClient _api;

  @override
  Future<Page<Salon>> listSalons(PageQuery query) => _api.get(
    Endpoints.salons,
    (body) => Page.fromJson(asJson(body), Salon.fromJson),
    query: query.toQuery(),
  );

  @override
  Future<Salon> getSalon(int id) =>
      _api.get(Endpoints.salon(id), (body) => Salon.fromJson(asJson(body)));
}
