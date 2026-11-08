import '../fake_call.dart';
import '../sample_data.dart';
import 'auth_repository.dart';
import 'auth_user.dart';

/// [AuthRepository] without the API, with one account: [user], signed in with [password].
class FakeAuthRepository implements AuthRepository {
  FakeAuthRepository({
    this.user = SampleData.client,
    this.password = SampleData.clientPassword,
    this.latency = Duration.zero,
    this._signedIn = false,
  });

  final AuthUser user;
  final String password;
  final Duration latency;

  bool _signedIn;
  int _failedTries = 0;

  /// Wrong passwords in a row before signing in is refused for a while, as the API does.
  static const maxFailedTries = 5;

  @override
  Future<AuthUser?> currentUser() =>
      fakeCall(latency, () => _signedIn ? user : null);

  @override
  Future<AuthUser> signIn({required String email, required String password}) =>
      fakeCall(latency, () {
        if (_failedTries >= maxFailedTries) {
          throw fakeError(429);
        }
        if (email.trim().toLowerCase() != user.email.toLowerCase() ||
            password != this.password) {
          _failedTries++;
          throw fakeError(401, 'Wrong email or password.');
        }
        _failedTries = 0;
        _signedIn = true;
        return user;
      });

  @override
  Future<void> signOut() => fakeCall(latency, () => _signedIn = false);

  @override
  Future<void> requestPasswordReset(String email) => fakeCall(latency, () {
    if (!email.contains('@')) {
      throw fakeError(400, 'Enter a full email address, like name@example.com');
    }
  });
}
