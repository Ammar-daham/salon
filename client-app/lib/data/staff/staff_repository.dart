import '../../api/api_client.dart';
import '../../api/endpoints.dart';
import 'staff_member.dart';

/// Who works at salons.
abstract interface class StaffRepository {
  /// A salon's team.
  Future<List<StaffMember>> listStaff(int salonId);
}

class ApiStaffRepository implements StaffRepository {
  const ApiStaffRepository(this._api);

  final ApiClient _api;

  @override
  Future<List<StaffMember>> listStaff(int salonId) => _api.get(
    Endpoints.staff(salonId),
    (body) => asList(body, StaffMember.fromJson),
  );
}
