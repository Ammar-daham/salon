package com.example.salon.dto;

import com.example.salon.model.TimeOff;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * The body of {@code POST /api/v1/businesses/{businessId}/staff/{staffId}/time-off} and of {@code PUT} on
 * one absence.
 *
 * <p>{@code starts_at} and {@code ends_at} are required, as {@code yyyy-MM-dd'T'HH:mm} on the salon's
 * clock, such as {@code 2026-12-24T09:00}, and {@code ends_at} must be after {@code starts_at}; a whole day
 * off runs from 00:00 to 00:00 the next day. {@code note} is optional, at most 255 characters; it's
 * trimmed, and a blank one counts as none. A {@code PUT} replaces every field.
 */
public record TimeOffRequest(
		@NotNull @JsonProperty("starts_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startsAt,
		@NotNull @JsonProperty("ends_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endsAt,
		@Size(max = 255) String note)
{
	public TimeOff toTimeOff()
	{
		return new TimeOff(null, startsAt, endsAt, note == null || note.isBlank() ? null : note.trim(), null, null);
	}
}
