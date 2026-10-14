package com.example.salon.dto;

import com.example.salon.model.WorkingInterval;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * The body of {@code PUT /api/v1/businesses/{businessId}/staff/{staffId}/schedule}: the staff member's
 * whole week, replacing what is stored, shaped like {@link BusinessHoursRequest}.
 *
 * <p>{@code hours} is required and may be empty: a day that isn't listed is a day off. Each shift has a
 * {@code day_of_week} ({@code MONDAY} to {@code SUNDAY}), {@code starts_at} and {@code ends_at}, all
 * required, the times as {@code HH:mm} on the salon's clock. A shift must end after it starts, and a
 * day's shifts may touch but not overlap.
 */
public record StaffScheduleRequest(@NotNull List<@NotNull @Valid Interval> hours)
{
	public record Interval(
			@NotNull @JsonProperty("day_of_week") DayOfWeek dayOfWeek,
			@NotNull @JsonProperty("starts_at") @JsonFormat(pattern = "HH:mm") LocalTime startsAt,
			@NotNull @JsonProperty("ends_at") @JsonFormat(pattern = "HH:mm") LocalTime endsAt)
	{
	}

	public List<WorkingInterval> toWorkingIntervals()
	{
		return hours.stream()
				.map(interval -> new WorkingInterval(interval.dayOfWeek(), interval.startsAt(), interval.endsAt()))
				.toList();
	}
}
