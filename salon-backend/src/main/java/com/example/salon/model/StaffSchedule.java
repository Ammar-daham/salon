package com.example.salon.model;

import java.util.List;

/**
 * A staff member's working week, as {@code GET} and {@code PUT} on
 * {@code /api/v1/businesses/{businessId}/staff/{staffId}/schedule} return it: {@code hours}, ordered by day
 * and then start, with the salon's {@code timezone} they are in.
 */
public record StaffSchedule(String timezone, List<WorkingInterval> hours)
{
}
