import '../api/api_exception.dart';

/// Answers with [answer] after [latency], as a call to the API would, so a screen built on a fake
/// shows its loading state.
Future<T> fakeCall<T>(Duration latency, T Function() answer) async {
  if (latency > Duration.zero) {
    await Future<void>.delayed(latency);
  }
  return answer();
}

/// The [ApiException] the API's answer with [status] would give.
ApiException fakeError(int status, [String? message]) {
  final kind = ApiErrorKind.fromStatus(status);
  return ApiException(
    kind,
    message: message ?? kind.fallbackMessage,
    status: status,
  );
}
