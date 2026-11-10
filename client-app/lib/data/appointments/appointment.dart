import '../../api/api_client.dart';
import '../salon_time.dart';
import '../salons/salon.dart';

/// Where an appointment is: booked, then optionally confirmed by the salon, then one of the last
/// three, which are final.
enum AppointmentStatus {
  booked('BOOKED'),
  confirmed('CONFIRMED'),
  completed('COMPLETED'),
  cancelled('CANCELLED'),
  noShow('NO_SHOW');

  const AppointmentStatus(this.apiValue);

  final String apiValue;

  static AppointmentStatus fromApi(String value) => values.firstWhere(
    (status) => status.apiValue == value,
    orElse: () => throw FormatException('Not an appointment status', value),
  );

  /// Whether it can still be changed or cancelled.
  bool get isOpen => this == booked || this == confirmed;
}

/// Someone an appointment is with, still named after they've left.
class PersonRef {
  const PersonRef({
    required this.id,
    required this.firstName,
    required this.lastName,
  });

  factory PersonRef.fromJson(Json json) => PersonRef(
    id: json['id']! as int,
    firstName: json['first_name']! as String,
    lastName: json['last_name']! as String,
  );

  final int id;
  final String firstName;
  final String lastName;

  String get name => '$firstName $lastName';
}

/// What an appointment needs to know about its salon.
class SalonRef {
  const SalonRef({
    required this.id,
    required this.name,
    required this.currency,
    required this.timezone,
  });

  SalonRef.of(Salon salon)
    : this(
        id: salon.id,
        name: salon.name,
        currency: salon.currency,
        timezone: salon.timezone,
      );

  final int id;
  final String name;

  /// The ISO 4217 code an appointment's price is in.
  final String currency;

  /// The IANA time zone an appointment's times are on.
  final String timezone;
}

/// One service, with one staff member, at one salon.
class Appointment {
  const Appointment({
    required this.id,
    required this.salon,
    required this.staff,
    required this.serviceId,
    required this.serviceName,
    required this.startsAt,
    required this.endsAt,
    required this.status,
    required this.price,
    this.notes,
  });

  /// From the API's appointment, which doesn't carry its salon, so the caller gives [salon].
  factory Appointment.fromJson(Json json, {required SalonRef salon}) {
    final service = asJson(json['service']);
    return Appointment(
      id: json['id']! as int,
      salon: salon,
      staff: PersonRef.fromJson(asJson(json['staff'])),
      serviceId: service['id']! as int,
      serviceName: service['name']! as String,
      startsAt: parseSalonDateTime(json['starts_at']! as String),
      endsAt: parseSalonDateTime(json['ends_at']! as String),
      status: AppointmentStatus.fromApi(json['status']! as String),
      price: (json['price']! as num).toDouble(),
      notes: json['notes'] as String?,
    );
  }

  final int id;
  final SalonRef salon;
  final PersonRef staff;
  final int serviceId;
  final String serviceName;

  /// On the salon's clock (see `salon_time.dart`).
  final DateTime startsAt;

  /// Set by the salon from how long the service takes.
  final DateTime endsAt;

  final AppointmentStatus status;

  /// What the service cost when it was booked, in the salon's currency.
  final double price;

  /// The client's note for the salon.
  final String? notes;

  Appointment copyWith({AppointmentStatus? status}) => Appointment(
    id: id,
    salon: salon,
    staff: staff,
    serviceId: serviceId,
    serviceName: serviceName,
    startsAt: startsAt,
    endsAt: endsAt,
    status: status ?? this.status,
    price: price,
    notes: notes,
  );
}
