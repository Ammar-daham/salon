/// Every screen's location. No location is written anywhere else.
abstract final class Routes {
  // Signing in.
  static const welcome = '/welcome';
  static const signIn = '/sign-in';
  static const signUp = '/sign-up';

  /// Sign in, then go back to [from].
  static String signInFrom(String from) =>
      Uri(path: signIn, queryParameters: {'from': from}).toString();

  // The Discover tab.
  static const home = '/';
  static const search = '/search';
  static const results = '/results';
  static const map = '/results/map';
  static const filters = '/results/filters';
  static const notifications = '/notifications';

  /// Salons found for [q].
  static String resultsFor(String q) =>
      Uri(path: results, queryParameters: {'q': q}).toString();

  static String salon(int salonId) => '/salons/$salonId';
  static String salonAbout(int salonId) => '${salon(salonId)}/about';
  static String salonReviews(int salonId) => '${salon(salonId)}/reviews';

  // Booking: who with, when, then a last look.
  static String bookStaff(int salonId, int serviceId) =>
      '${salon(salonId)}/book/$serviceId';

  /// [staffId] null is anyone free.
  static String bookTime(int salonId, int serviceId, {int? staffId}) =>
      _withStaff('${bookStaff(salonId, serviceId)}/time', staffId);

  static String bookReview(int salonId, int serviceId, {int? staffId}) =>
      _withStaff('${bookStaff(salonId, serviceId)}/review', staffId);

  /// After booking: the appointment, awaiting the salon's confirmation.
  static String booked(int appointmentId) => '/booked/$appointmentId';

  // The Bookings tab.
  static const bookings = '/bookings';
  static String booking(int appointmentId) => '$bookings/$appointmentId';
  static String cancelBooking(int appointmentId) =>
      '${booking(appointmentId)}/cancel';
  static String reviewVisit(int appointmentId) =>
      '${booking(appointmentId)}/review';

  // The other tabs.
  static const favourites = '/favourites';
  static const profile = '/profile';
  static const editProfile = '/profile/edit';

  // For development, from debug builds only.
  static const screens = '/dev/screens';
  static const gallery = '/dev/gallery';

  static String _withStaff(String path, int? staffId) => staffId == null
      ? path
      : Uri(path: path, queryParameters: {'staff': '$staffId'}).toString();
}
