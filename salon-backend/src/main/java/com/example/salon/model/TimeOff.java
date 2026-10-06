package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A staff member's absence. The database stores two instants; the API reads and writes them as
 * date-times on the salon's clock (businesses.timezone), which is how the salon thinks about them.
 */
public record TimeOff(
        Long id,
        @JsonProperty("starts_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startsAt,
        @JsonProperty("ends_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endsAt,
        String note,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt)
{
}
