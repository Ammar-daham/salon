package com.example.salon.paging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** One page of a list, and how long the whole list is (BE-15). A page past the end has no items. */
public record Page<T>(
		List<T> items,
		int page,
		int size,
		@JsonProperty("total_items") long totalItems,
		@JsonProperty("total_pages") long totalPages)
{
	public static <T> Page<T> of(List<T> items, PageQuery query, long totalItems)
	{
		long totalPages = (totalItems + query.size() - 1) / query.size();
		return new Page<>(items, query.page(), query.size(), totalItems, totalPages);
	}

	public static <T> Page<T> empty(PageQuery query)
	{
		return of(List.of(), query, 0);
	}
}
