import 'package:dio/dio.dart';

import '../config.dart';
import 'api_exception.dart';

/// A JSON object as the API sends it.
typedef Json = Map<String, Object?>;

/// Reads an answer's body into what the caller wants. Throws a [TypeError] or [FormatException]
/// for a body without the expected shape.
typedef Parse<T> = T Function(Object? body);

/// Calls salon-backend. Every failure comes out as an [ApiException]: a failed call by its HTTP
/// status, no answer as [ApiErrorKind.network], and an answer that can't be read as
/// [ApiErrorKind.badResponse].
class ApiClient {
  ApiClient(AppConfig config, {HttpClientAdapter? adapter})
    : dio = Dio(
        BaseOptions(
          baseUrl: config.apiBaseUrl,
          connectTimeout: const Duration(seconds: 10),
          receiveTimeout: const Duration(seconds: 20),
          headers: {'Accept': 'application/json'},
          responseType: ResponseType.json,
        ),
      ) {
    if (adapter != null) {
      dio.httpClientAdapter = adapter;
    }
  }

  /// For adding to every call, such as signing in with a token.
  final Dio dio;

  Future<T> get<T>(String path, Parse<T> parse, {Map<String, Object>? query}) =>
      _call(() => dio.get<Object?>(path, queryParameters: query), parse);

  Future<T> post<T>(String path, Parse<T> parse, {Object? body}) =>
      _call(() => dio.post<Object?>(path, data: body), parse);

  Future<T> put<T>(String path, Parse<T> parse, {Object? body}) =>
      _call(() => dio.put<Object?>(path, data: body), parse);

  Future<void> delete(String path) =>
      _call(() => dio.delete<Object?>(path), (_) {});

  Future<T> _call<T>(
    Future<Response<Object?>> Function() send,
    Parse<T> parse,
  ) async {
    final Response<Object?> response;
    try {
      response = await send();
    } on DioException catch (error) {
      throw ApiException.fromDio(error);
    }
    try {
      return parse(response.data);
    } on TypeError catch (error) {
      throw ApiException.badResponse(error);
    } on FormatException catch (error) {
      throw ApiException.badResponse(error);
    }
  }
}

/// [body] as a JSON object.
Json asJson(Object? body) => body! as Json;

/// [body] as a JSON list of objects, each read with [item].
List<T> asList<T>(Object? body, T Function(Json json) item) =>
    (body! as List<Object?>).map((e) => item(asJson(e))).toList();
