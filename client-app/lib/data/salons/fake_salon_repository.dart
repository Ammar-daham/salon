import '../../api/paging.dart';
import '../fake_call.dart';
import '../sample_data.dart';
import 'salon.dart';
import 'salon_repository.dart';

/// [SalonRepository] without the API, searching and paging [salons] the way the API does.
class FakeSalonRepository implements SalonRepository {
  FakeSalonRepository({
    this.salons = SampleData.salons,
    this.latency = Duration.zero,
  });

  final List<Salon> salons;
  final Duration latency;

  @override
  Future<Page<Salon>> listSalons(PageQuery query) => fakeCall(latency, () {
    final q = query.q?.trim().toLowerCase() ?? '';
    final found = [
      for (final salon in salons)
        if (q.isEmpty ||
            salon.name.toLowerCase().contains(q) ||
            (salon.description?.toLowerCase().contains(q) ?? false) ||
            salon.addresses.any((a) => a.city.toLowerCase().contains(q)))
          salon,
    ];
    final sort = query.sort ?? 'name';
    final descending = sort.startsWith('-');
    // The API's created_at order; the fakes' ids go up as salons are added.
    found.sort(
      (a, b) => sort.endsWith('name')
          ? a.name.toLowerCase().compareTo(b.name.toLowerCase())
          : a.id.compareTo(b.id),
    );
    final ordered = descending ? found.reversed.toList() : found;

    final page = query.page ?? 1;
    final size = query.size ?? 20;
    if (page < 1 || size < 1 || size > PageQuery.maxPageSize) {
      throw fakeError(400);
    }
    final start = (page - 1) * size;
    return Page(
      items: ordered.skip(start).take(size).toList(),
      page: page,
      size: size,
      totalItems: ordered.length,
      totalPages: (ordered.length / size).ceil(),
    );
  });

  @override
  Future<Salon> getSalon(int id) => fakeCall(
    latency,
    () => salons.firstWhere(
      (salon) => salon.id == id,
      orElse: () => throw fakeError(404),
    ),
  );
}
