package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * One shift of {@link StaffSchedule}: the staff member works on {@code day_of_week} ({@code MONDAY} to
 * {@code SUNDAY}) from {@code starts_at} until {@code ends_at}, as {@code HH:mm} on the salon's clock.
 */
public record WorkingInterval(
        @JsonProperty("day_of_week") DayOfWeek dayOfWeek,
        @JsonProperty("starts_at") @JsonFormat(pattern = "HH:mm") LocalTime startsAt,
        @JsonProperty("ends_at") @JsonFormat(pattern = "HH:mm") LocalTime endsAt)
{
}
