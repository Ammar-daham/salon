import '../../api/api_client.dart';

/// A salon, with what the app shows about it: where it is, how to reach it and what it offers.
class Salon {
  const Salon({
    required this.id,
    required this.name,
    required this.description,
    required this.image,
    required this.currency,
    required this.timezone,
    required this.addresses,
    required this.contacts,
    required this.services,
  });

  /// From the API's salon, which it calls a business.
  factory Salon.fromJson(Json json) => Salon(
    id: json['id']! as int,
    name: json['name']! as String,
    description: json['description'] as String?,
    image: json['image']! as String,
    currency: json['currency']! as String,
    timezone: json['timezone']! as String,
    addresses: asList(json['addresses'] ?? const [], Address.fromJson),
    contacts: asList(json['contacts'] ?? const [], Contact.fromJson),
    services: asList(json['services'] ?? const [], SalonService.fromJson),
  );

  final int id;
  final String name;
  final String? description;

  /// A URL or a data URI for its photo.
  final String image;

  /// The ISO 4217 code its prices are in, such as EUR.
  final String currency;

  /// The IANA time zone its hours and appointments are on, such as Europe/Berlin.
  final String timezone;

  final List<Address> addresses;
  final List<Contact> contacts;
  final List<SalonService> services;

  /// Where the salon is: its first address, if it has one.
  Address? get address => addresses.firstOrNull;

  /// Its first phone number, if it has one.
  String? get phone =>
      contacts.where((c) => c.type.toLowerCase() == 'phone').firstOrNull?.value;
}

class Address {
  const Address({
    required this.id,
    required this.street,
    required this.city,
    required this.country,
    this.postalCode,
    this.latitude,
    this.longitude,
  });

  factory Address.fromJson(Json json) => Address(
    id: json['id']! as int,
    street: json['street']! as String,
    city: json['city']! as String,
    country: json['country']! as String,
    postalCode: json['postal_code'] as String?,
    latitude: (json['latitude'] as num?)?.toDouble(),
    longitude: (json['longitude'] as num?)?.toDouble(),
  );

  final int id;
  final String street;
  final String city;
  final String country;
  final String? postalCode;

  /// Both set, or both null.
  final double? latitude;
  final double? longitude;
}

/// A way to reach a salon, such as its phone number.
class Contact {
  const Contact({required this.id, required this.type, required this.value});

  factory Contact.fromJson(Json json) => Contact(
    id: json['id']! as int,
    type: json['type']! as String,
    value: json['value']! as String,
  );

  final int id;

  /// Free text, such as "phone" or "email".
  final String type;
  final String value;
}

/// Something a salon offers to book, such as a haircut.
class SalonService {
  const SalonService({
    required this.id,
    required this.name,
    required this.description,
    required this.durationMinutes,
    required this.price,
    required this.isActive,
  });

  factory SalonService.fromJson(Json json) => SalonService(
    id: json['id']! as int,
    name: json['name']! as String,
    description: json['description'] as String?,
    durationMinutes: json['duration_minutes']! as int,
    price: (json['price']! as num).toDouble(),
    isActive: json['is_active']! as bool,
  );

  final int id;
  final String name;
  final String? description;
  final int durationMinutes;

  /// In the salon's currency.
  final double price;

  /// Whether it can be booked.
  final bool isActive;
}
