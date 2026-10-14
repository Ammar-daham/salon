package com.example.salon.dto;

import com.example.salon.model.OpeningInterval;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * The body of {@code PUT /api/v1/businesses/{businessId}/hours}: the salon's whole week, replacing what is
 * stored.
 *
 * <p>{@code hours} is required and may be empty: a day that isn't listed is closed, and an empty list
 * closes the salon every day. Each interval has a {@code day_of_week} ({@code MONDAY} to {@code SUNDAY}),
 * {@code opens_at} and {@code closes_at}, all required, the times as {@code HH:mm} on the salon's own
 * clock. An interval must close after it opens, and a day's intervals may touch but not overlap.
 */
public record BusinessHoursRequest(@NotNull List<@NotNull @Valid Interval> hours)
{
	public record Interval(
			@NotNull @JsonProperty("day_of_week") DayOfWeek dayOfWeek,
			@NotNull @JsonProperty("opens_at") @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
			@NotNull @JsonProperty("closes_at") @JsonFormat(pattern = "HH:mm") LocalTime closesAt)
	{
	}

	public List<OpeningInterval> toOpeningIntervals()
	{
		return hours.stream()
				.map(interval -> new OpeningInterval(interval.dayOfWeek(), interval.opensAt(), interval.closesAt()))
				.toList();
	}
}
