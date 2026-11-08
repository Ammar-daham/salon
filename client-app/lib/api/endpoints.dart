/// Every salon-backend path the app calls. No path is written anywhere else.
abstract final class Endpoints {
  static const salons = '/api/v1/businesses';
  static String salon(int id) => '$salons/$id';

  /// The salon's week of opening hours.
  static String openingHours(int salonId) => '${salon(salonId)}/hours';

  static String staff(int salonId) => '${salon(salonId)}/staff';

  /// When a service can be booked, per staff member who performs it.
  static String availability(int salonId, int serviceId) =>
      '${salon(salonId)}/services/$serviceId/availability';
}
