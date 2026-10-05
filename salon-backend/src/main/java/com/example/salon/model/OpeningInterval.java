package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** One row of business_hours: the salon is open on this weekday from opensAt until closesAt, its own wall clock. */
public record OpeningInterval(
        @JsonProperty("day_of_week") DayOfWeek dayOfWeek,
        @JsonProperty("opens_at") @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
        @JsonProperty("closes_at") @JsonFormat(pattern = "HH:mm") LocalTime closesAt)
{
}
