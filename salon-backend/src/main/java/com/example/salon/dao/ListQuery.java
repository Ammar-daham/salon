package com.example.salon.dao;

import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A list query's WHERE clause, run as one page: a count, then that page's rows. Every piece of
 * SQL handed to it is a constant written in a DAO; what a client sent only ever travels as a
 * parameter (BE-26).
 */
final class ListQuery
{
	private final List<String> conditions = new ArrayList<>();
	private final List<Object> args = new ArrayList<>();

	ListQuery where(String condition, Object... values)
	{
		conditions.add(condition);
		args.addAll(Arrays.asList(values));
		return this;
	}

	/** An ILIKE pattern for rows that contain the search term anywhere, or null when there's no term. */
	static String containing(String search)
	{
		if (search == null || search.isBlank()) {
			return null;
		}
		String literal = search.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + literal + "%";
	}

	/** ORDER BY the given columns one way, then by id, so rows that tie keep a fixed place across pages. */
	static String orderBy(List<String> columns, boolean descending, String idColumn)
	{
		String direction = descending ? " DESC NULLS LAST" : " ASC NULLS LAST";
		return Stream.concat(columns.stream(), Stream.of(idColumn))
				.map(column -> column + direction)
				.collect(Collectors.joining(", "));
	}

	<T> Page<T> page(JdbcTemplate jdbcTemplate, String select, String from, String orderBy, PageQuery page,
			RowMapper<T> mapper)
	{
		String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
		Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM " + from + where, Long.class, args.toArray());
		if (total == null || page.offset() >= total) {
			return Page.of(List.of(), page, total == null ? 0 : total);
		}
		List<Object> pageArgs = new ArrayList<>(args);
		pageArgs.add(page.size());
		pageArgs.add(page.offset());
		List<T> items = jdbcTemplate.query(
				"SELECT " + select + " FROM " + from + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?",
				mapper, pageArgs.toArray());
		return Page.of(items, page, total);
	}
}
