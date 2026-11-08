import 'auth_user.dart';

/// Signing in and out.
///
/// Only `FakeAuthRepository` for now: the app signs in with a token, which salon-backend gets in
/// task B2 of the plan. Its cookie session suits a browser, not an app.
abstract interface class AuthRepository {
  /// Who is signed in, or null when no one is.
  Future<AuthUser?> currentUser();

  /// Fails as unauthorized for a wrong email or password, and as too many requests after too
  /// many tries.
  Future<AuthUser> signIn({required String email, required String password});

  Future<void> signOut();

  /// Emails a link to set a new password. Succeeds whether or not [email] has an account, so the
  /// answer can't tell anyone which emails do.
  Future<void> requestPasswordReset(String email);
}
