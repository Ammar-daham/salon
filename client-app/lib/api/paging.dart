import 'api_client.dart';

/// One page of a list, and how long the whole list is.
class Page<T> {
  const Page({
    required this.items,
    required this.page,
    required this.size,
    required this.totalItems,
    required this.totalPages,
  });

  /// From the API's `{items, page, size, total_items, total_pages}`, each item read with [item].
  factory Page.fromJson(Json json, T Function(Json json) item) => Page(
    items: asList(json['items'], item),
    page: json['page']! as int,
    size: json['size']! as int,
    totalItems: json['total_items']! as int,
    totalPages: json['total_pages']! as int,
  );

  final List<T> items;

  /// Which page this is, counting from 1.
  final int page;

  /// How many items a page holds.
  final int size;

  final int totalItems;
  final int totalPages;

  /// Whether there's a page after this one.
  bool get hasMore => page < totalPages;
}

/// What a paged list is asked for. Leaving a field out leaves it to the API: the first page, 20
/// items a page, no search, and the list's own order.
class PageQuery {
  const PageQuery({this.page, this.size, this.q, this.sort});

  /// The page wanted, counting from 1.
  final int? page;

  /// How many items a page holds, 1 to [maxPageSize].
  final int? size;

  /// Text to search for, in what the list searches.
  final String? q;

  /// One of the list's sort keys, with a leading "-" for the other way round.
  final String? sort;

  /// The most items the API sends in one page.
  static const maxPageSize = 100;

  /// The query string, with [filters] under their API names. A blank search and filters that
  /// aren't set are left out rather than sent empty, so the API doesn't filter on them.
  Map<String, Object> toQuery([Map<String, Object?> filters = const {}]) {
    final q = this.q?.trim();
    return {
      if (q != null && q.isNotEmpty) 'q': q,
      'sort': ?sort,
      'page': ?page,
      'size': ?size,
      for (final MapEntry(:key, :value) in filters.entries)
        if (value != null && value != '') key: value,
    };
  }
}
