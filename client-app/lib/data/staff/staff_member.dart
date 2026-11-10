import '../../api/api_client.dart';

/// Someone who works at a salon.
class StaffMember {
  const StaffMember({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.title,
    required this.isActive,
  });

  factory StaffMember.fromJson(Json json) => StaffMember(
    id: json['id']! as int,
    firstName: json['first_name']! as String,
    lastName: json['last_name']! as String,
    title: json['title']! as String,
    isActive: json['is_active']! as bool,
  );

  /// The staff id a booking is made with, not their user id.
  final int id;

  final String firstName;
  final String lastName;

  /// What they do, such as Senior Stylist.
  final String title;

  /// Whether they can be booked.
  final bool isActive;

  String get name => '$firstName $lastName';
}
