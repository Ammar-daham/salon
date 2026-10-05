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
 * Body of PUT /businesses/{businessId}/hours: the whole week, replacing what is stored. A day that isn't
 * listed is closed, and an empty list closes the salon every day. Times are "HH:mm" on the salon's own clock.
 * BusinessHoursService checks that each interval ends after it starts and that a day's intervals don't overlap.
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
