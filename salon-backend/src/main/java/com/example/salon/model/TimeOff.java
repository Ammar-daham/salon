package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A staff member's absence, as the endpoints under
 * {@code /api/v1/businesses/{businessId}/staff/{staffId}/time-off} return it.
 *
 * <p>The database stores two instants; the API reads and writes them as date-times on the salon's clock,
 * such as {@code 2026-12-24T00:00}, which is how the salon thinks about them. So if the salon's
 * {@code timezone} changes, an absence keeps its moment and reads differently. {@code note} is null when
 * there's none, and {@code updated_at} until the absence is first changed.
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
