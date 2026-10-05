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
 * Body of PUT /businesses/{businessId}/staff/{staffId}/schedule: the whole week, replacing what is stored,
 * shaped like BusinessHoursRequest. A day that isn't listed is a day off. Times are "HH:mm", salon time.
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
