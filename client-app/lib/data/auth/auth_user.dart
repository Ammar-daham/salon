import '../../api/api_client.dart';

/// What someone may do. The app is for clients; the others are a salon's staff and the platform's
/// admins, who use the admin panel.
enum Role {
  customer('CUSTOMER'),
  employee('EMPLOYEE'),
  admin('ADMIN'),
  superAdmin('SUPER_ADMIN');

  const Role(this.apiValue);

  final String apiValue;

  static Role fromApi(String value) => values.firstWhere(
    (role) => role.apiValue == value,
    orElse: () => throw FormatException('Not a role', value),
  );
}

/// Whoever is signed in.
class AuthUser {
  const AuthUser({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.email,
    required this.role,
  });

  factory AuthUser.fromJson(Json json) => AuthUser(
    id: json['id']! as int,
    firstName: json['first_name']! as String,
    lastName: json['last_name']! as String,
    email: json['email']! as String,
    role: Role.fromApi(json['role']! as String),
  );

  final int id;
  final String firstName;
  final String lastName;
  final String email;
  final Role role;

  String get name => '$firstName $lastName';
}
