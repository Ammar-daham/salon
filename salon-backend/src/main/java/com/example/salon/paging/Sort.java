package com.example.salon.paging;

import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;

import java.util.EnumSet;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A list's order from ?sort=: one of the list's keys, such as created_at, or -created_at for
 * the other way round. The keys are an enum each DAO turns into its own SQL, so nothing a client
 * sends is ever part of a query.
 */
public record Sort<K extends Enum<K>>(K key, boolean descending)
{
	public static <K extends Enum<K>> Sort<K> parse(String value, Class<K> keys)
	{
		return parse(value, EnumSet.allOf(keys));
	}

	/** For a list that takes only some of its DAO's keys; anything else is a 400 naming those it takes. */
	public static <K extends Enum<K>> Sort<K> parse(String value, EnumSet<K> keys)
	{
		boolean descending = value.startsWith("-");
		String name = descending ? value.substring(1) : value;
		for (K key : keys) {
			if (paramName(key).equals(name)) {
				return new Sort<>(key, descending);
			}
		}
		String allowed = keys.stream()
				.map(Sort::paramName)
				.flatMap(key -> Stream.of(key, "-" + key))
				.collect(Collectors.joining(", "));
		throw new BaseException("sort must be one of " + allowed, ErrorCode.BAD_REQUEST);
	}

	private static String paramName(Enum<?> key)
	{
		return key.name().toLowerCase(Locale.ROOT);
	}
}
