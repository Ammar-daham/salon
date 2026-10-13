package com.example.salon.model;

import java.util.List;

/**
 * A salon's weekly opening hours, as {@code GET} and {@code PUT} on {@code /api/v1/businesses/{businessId}/hours}
 * return them: {@code hours}, ordered by day and then opening time. The times are only meaningful together
 * with the salon's {@code timezone}, so it comes along.
 */
public record BusinessHours(String timezone, List<OpeningInterval> hours)
{
}
