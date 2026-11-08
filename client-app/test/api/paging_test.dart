import 'package:flutter_test/flutter_test.dart';
import 'package:salonat_client/api/paging.dart';

void main() {
  test('reads a page and its items', () {
    final page = Page.fromJson({
      'items': [
        {'n': 1},
        {'n': 2},
      ],
      'page': 1,
      'size': 2,
      'total_items': 5,
      'total_pages': 3,
    }, (json) => json['n']! as int);

    expect(page.items, [1, 2]);
    expect(page.page, 1);
    expect(page.size, 2);
    expect(page.totalItems, 5);
    expect(page.totalPages, 3);
    expect(page.hasMore, isTrue);
  });

  test('the last page has no more after it', () {
    final page = Page.fromJson({
      'items': <Object>[],
      'page': 3,
      'size': 2,
      'total_items': 5,
      'total_pages': 3,
    }, (json) => json);

    expect(page.hasMore, isFalse);
  });

  test('an empty list has no more pages', () {
    final page = Page.fromJson({
      'items': <Object>[],
      'page': 1,
      'size': 20,
      'total_items': 0,
      'total_pages': 0,
    }, (json) => json);

    expect(page.hasMore, isFalse);
  });

  group('PageQuery', () {
    test('sends what is set, under the API names', () {
      const query = PageQuery(page: 2, size: 10, q: ' glow ', sort: '-name');

      expect(query.toQuery({'status': 'APPROVED'}), {
        'q': 'glow',
        'sort': '-name',
        'page': 2,
        'size': 10,
        'status': 'APPROVED',
      });
    });

    test('leaves out a blank search and filters that are not set', () {
      const query = PageQuery(q: '   ');

      expect(query.toQuery({'status': null, 'city': ''}), isEmpty);
    });
  });
}
