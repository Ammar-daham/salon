import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/api/api_client.dart';
import 'package:salonat_client/api/api_exception.dart';

import '../support/fake_http.dart';

/// The API's error body.
Map<String, Object?> errorBody(String message) => {
  'errorCode': 'NOT_FOUND',
  'message': message,
  'status': 'NOT_FOUND',
  'timestamp': '2026-11-08T10:00:00Z',
  'path': '/api/v1/businesses/9',
};

Future<ApiException> failure(Future<Object?> call) async {
  try {
    await call;
  } on ApiException catch (error) {
    return error;
  }
  fail('Expected an ApiException');
}

void main() {
  test('calls the API at its base URL and reads the answer', () async {
    final http = FakeHttp.always(200, {'name': 'Glow'});

    final name = await http.client.get(
      '/api/v1/businesses/1',
      (body) => asJson(body)['name']! as String,
    );

    expect(name, 'Glow');
    expect(
      http.requests.single.uri.toString(),
      'http://api.test/api/v1/businesses/1',
    );
  });

  test('a failed call says what went wrong, from the error body', () async {
    final http = FakeHttp.always(404, errorBody('Business not found'));

    final error = await failure(http.client.get('/x', (body) => body));

    expect(error.kind, ApiErrorKind.notFound);
    expect(error.status, 404);
    expect(error.message, 'Business not found');
    expect(error.errorCode, 'NOT_FOUND');
    expect(error.path, '/api/v1/businesses/9');
  });

  test('takes the kind from the HTTP status, not the body', () async {
    for (final (status, kind) in [
      (400, ApiErrorKind.validation),
      (401, ApiErrorKind.unauthorized),
      (403, ApiErrorKind.forbidden),
      (409, ApiErrorKind.conflict),
      (422, ApiErrorKind.validation),
      (429, ApiErrorKind.tooManyRequests),
      (500, ApiErrorKind.server),
      (503, ApiErrorKind.server),
      (418, ApiErrorKind.unknown),
    ]) {
      final http = FakeHttp.always(status, errorBody('x'));
      final error = await failure(http.client.get('/x', (body) => body));
      expect(error.kind, kind, reason: '$status');
    }
  });

  test('falls back to its own message when the body has none', () async {
    for (final body in [
      null,
      '',
      {'message': '  '},
      'Bad Gateway',
    ]) {
      final http = FakeHttp.always(502, body);
      final error = await failure(http.client.get('/x', (body) => body));
      expect(error.kind, ApiErrorKind.server);
      expect(
        error.message,
        ApiErrorKind.server.fallbackMessage,
        reason: '$body',
      );
    }
  });

  test('no answer at all is a network error', () async {
    final http = FakeHttp.offline();

    final error = await failure(http.client.get('/x', (body) => body));

    expect(error.kind, ApiErrorKind.network);
    expect(error.status, isNull);
    expect(error.message, ApiErrorKind.network.fallbackMessage);
  });

  test('an answer without the expected shape is a bad response', () async {
    final http = FakeHttp.always(200, {'name': 42});

    final error = await failure(
      http.client.get('/x', (body) => asJson(body)['name']! as String),
    );

    expect(error.kind, ApiErrorKind.badResponse);
    expect(error.cause, isA<TypeError>());
  });

  test('sends a body as JSON', () async {
    final http = FakeHttp.always(201, {'ok': true});

    await http.client.post(
      '/x',
      (body) => body,
      body: {'first_name': 'Olivia'},
    );

    expect(http.requests.single.method, 'POST');
    expect(http.requests.single.data, {'first_name': 'Olivia'});
  });
}
