package com.example.salon.model;

import java.util.List;

/**
 * A salon's weekly opening hours, ordered by day and then opening time. The times are only meaningful
 * together with the salon's time zone, so it comes along.
 */
public record BusinessHours(String timezone, List<OpeningInterval> hours)
{
}
