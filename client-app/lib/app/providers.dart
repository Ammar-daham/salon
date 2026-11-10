import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/api_client.dart';
import '../config.dart';
import '../data/auth/auth_user.dart';
import '../data/repositories.dart';

/// The settings the app was built with. `main` overrides it.
final configProvider = Provider<AppConfig>(
  (ref) => throw UnimplementedError('Override configProvider in main'),
);

/// Where screens read their data, as [AppConfig.dataSource] says. Tests override it with
/// `Repositories.fake()`.
final repositoriesProvider = Provider<Repositories>((ref) {
  final config = ref.watch(configProvider);
  return switch (config.dataSource) {
    DataSource.fake => Repositories.fake(
      latency: const Duration(milliseconds: 400),
    ),
    DataSource.api => Repositories.api(ApiClient(config)),
  };
});

/// Who is signed in: null when no one is, and loading while the app finds out at start.
final sessionProvider = AsyncNotifierProvider<Session, AuthUser?>(Session.new);

class Session extends AsyncNotifier<AuthUser?> {
  @override
  Future<AuthUser?> build() =>
      ref.watch(repositoriesProvider).auth.currentUser();

  /// Signs in, or fails with the repository's error and stays signed out.
  Future<void> signIn({required String email, required String password}) async {
    final user = await ref
        .read(repositoriesProvider)
        .auth
        .signIn(email: email, password: password);
    state = AsyncData(user);
  }

  Future<void> signOut() async {
    await ref.read(repositoriesProvider).auth.signOut();
    state = const AsyncData(null);
  }
}
