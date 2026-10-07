package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * When a service can be booked, per staff member who performs it. Times are on the salon's clock, as on an
 * appointment, and each slot lasts the service's duration.
 */
public record Availability(
        String timezone,
        @JsonProperty("duration_minutes") int durationMinutes,
        List<StaffSlots> staff)
{
    public record StaffSlots(
            Long id,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            List<Slot> slots)
    {
    }

    public record Slot(
            @JsonProperty("starts_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startsAt,
            @JsonProperty("ends_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endsAt)
    {
    }
}
