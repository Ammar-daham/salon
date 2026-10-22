package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Something one user is told about, as {@code /api/v1/notifications} returns it. {@code type} is a
 * {@link NotificationType}, and {@code business} the salon it's about, with its {@code id} and its
 * {@code name} as it is now. {@code read_at} is null until the reader marks it read.
 */
public record Notification(
        Long id,
        NotificationType type,
        BusinessRef business,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("read_at") Instant readAt)
{
    public record BusinessRef(Long id, String name)
    {
    }
}
