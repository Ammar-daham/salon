import 'package:dio/dio.dart';

/// What went wrong with a call to the API, from the HTTP status of its answer.
enum ApiErrorKind {
  /// 401: not signed in, or the session ended.
  unauthorized('Your session has ended. Please sign in again.'),

  /// 403: signed in, but not allowed.
  forbidden('You can’t do that.'),

  /// 404.
  notFound('We couldn’t find that.'),

  /// 409, such as a time someone else booked first.
  conflict('That clashes with something that already exists.'),

  /// 400 or 422: the request was refused as it was.
  validation('Some details weren’t accepted. Please check them and try again.'),

  /// 429, such as too many sign-in attempts.
  tooManyRequests('Too many tries. Please wait a moment and try again.'),

  /// 5xx.
  server('Something went wrong on our side. Please try again.'),

  /// No answer at all: offline, the API is down, or it took too long.
  network('Couldn’t connect. Check your connection and try again.'),

  /// An answer the app couldn't read.
  badResponse('Something went wrong. Please try again.'),

  /// Any other status.
  unknown('Something went wrong. Please try again.');

  const ApiErrorKind(this.fallbackMessage);

  /// What to show when the API didn't say what went wrong.
  final String fallbackMessage;

  static ApiErrorKind fromStatus(int status) => switch (status) {
    401 => unauthorized,
    403 => forbidden,
    404 => notFound,
    409 => conflict,
    400 || 422 => validation,
    429 => tooManyRequests,
    >= 500 => server,
    _ => unknown,
  };
}

/// A call to the API that failed, with a [message] fit to show the client.
///
/// The API answers every error with `{errorCode, message, status, timestamp, path}`. Only
/// `message` is used for more than logging: `status` is the status's name, such as "NOT_FOUND",
/// and `errorCode` isn't a fixed list, so [kind] comes from the HTTP status instead.
class ApiException implements Exception {
  const ApiException(
    this.kind, {
    required this.message,
    this.status,
    this.errorCode,
    this.path,
    this.cause,
  });

  /// From what dio threw for a failed call.
  factory ApiException.fromDio(DioException error) {
    final cause = error.error;
    if (cause is ApiException) {
      return cause;
    }
    final response = error.response;
    final status = response?.statusCode;
    if (response == null || status == null) {
      return ApiException(
        ApiErrorKind.network,
        message: ApiErrorKind.network.fallbackMessage,
        cause: error,
      );
    }
    final kind = ApiErrorKind.fromStatus(status);
    final body = response.data;
    String? read(String key) => body is Map && body[key] is String
        ? (body[key] as String).trim()
        : null;
    final message = read('message');
    return ApiException(
      kind,
      message: message == null || message.isEmpty
          ? kind.fallbackMessage
          : message,
      status: status,
      errorCode: read('errorCode'),
      path: read('path'),
    );
  }

  /// For an answer that didn't have the shape the app expects.
  factory ApiException.badResponse(Object cause) => ApiException(
    ApiErrorKind.badResponse,
    message: ApiErrorKind.badResponse.fallbackMessage,
    cause: cause,
  );

  final ApiErrorKind kind;
  final String message;

  /// The HTTP status, or null when there was no answer.
  final int? status;

  final String? errorCode;

  /// The API path that failed, as the API reports it.
  final String? path;

  /// What failed underneath, for logs: the connection error, or why an answer couldn't be read.
  final Object? cause;

  @override
  String toString() => 'ApiException($kind, $status, $message, $cause)';
}
