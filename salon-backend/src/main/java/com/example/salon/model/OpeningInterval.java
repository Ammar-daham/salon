package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * One interval of {@link BusinessHours}: the salon is open on {@code day_of_week} ({@code MONDAY} to
 * {@code SUNDAY}) from {@code opens_at} until {@code closes_at}, as {@code HH:mm} on its own wall clock.
 */
public record OpeningInterval(
        @JsonProperty("day_of_week") DayOfWeek dayOfWeek,
        @JsonProperty("opens_at") @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
        @JsonProperty("closes_at") @JsonFormat(pattern = "HH:mm") LocalTime closesAt)
{
}
