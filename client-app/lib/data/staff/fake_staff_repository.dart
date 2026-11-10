import '../fake_call.dart';
import '../sample_data.dart';
import 'staff_member.dart';
import 'staff_repository.dart';

/// [StaffRepository] without the API, with the sample salons' teams.
class FakeStaffRepository implements StaffRepository {
  FakeStaffRepository({this.latency = Duration.zero});

  final Duration latency;

  @override
  Future<List<StaffMember>> listStaff(int salonId) => fakeCall(
    latency,
    () => SampleData.staff[salonId] ?? (throw fakeError(404)),
  );
}
