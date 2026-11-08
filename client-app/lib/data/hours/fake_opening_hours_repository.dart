import '../fake_call.dart';
import '../sample_data.dart';
import 'opening_hours.dart';
import 'opening_hours_repository.dart';

/// [OpeningHoursRepository] without the API, with the sample salons' hours.
class FakeOpeningHoursRepository implements OpeningHoursRepository {
  FakeOpeningHoursRepository({this.latency = Duration.zero});

  final Duration latency;

  @override
  Future<OpeningHours> getOpeningHours(int salonId) => fakeCall(
    latency,
    () => SampleData.hours[salonId] ?? (throw fakeError(404)),
  );
}
