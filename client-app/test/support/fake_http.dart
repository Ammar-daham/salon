import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:salonat_client/api/api_client.dart';
import 'package:salonat_client/config.dart';

/// What the fake server answers a request with.
typedef Answer = ({int status, Object? body});

/// An [ApiClient] whose calls go to [answer] instead of a server. [requests] records them.
class FakeHttp implements HttpClientAdapter {
  FakeHttp(this.answer);

  /// Answers every request with [status] and [body], as JSON.
  FakeHttp.always(int status, Object? body)
    : this((_) => (status: status, body: body));

  /// Fails every request as if the server couldn't be reached.
  FakeHttp.offline()
    : this((_) => throw const SocketException('Connection refused'));

  final Answer Function(RequestOptions request) answer;
  final requests = <RequestOptions>[];

  late final client = ApiClient(
    const AppConfig(apiBaseUrl: 'http://api.test'),
    adapter: this,
  );

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    final (:status, :body) = answer(options);
    // A string goes as plain text, like a proxy's error page; anything else as JSON.
    return ResponseBody.fromString(
      body is String ? body : jsonEncode(body),
      status,
      headers: {
        Headers.contentTypeHeader: [
          body is String
              ? Headers.textPlainContentType
              : Headers.jsonContentType,
        ],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
