package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** One row of staff_schedules: this staff member works on this weekday from startsAt until endsAt, salon time. */
public record WorkingInterval(
        @JsonProperty("day_of_week") DayOfWeek dayOfWeek,
        @JsonProperty("starts_at") @JsonFormat(pattern = "HH:mm") LocalTime startsAt,
        @JsonProperty("ends_at") @JsonFormat(pattern = "HH:mm") LocalTime endsAt)
{
}
