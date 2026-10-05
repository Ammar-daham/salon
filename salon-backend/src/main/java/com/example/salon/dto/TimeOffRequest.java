package com.example.salon.dto;

import com.example.salon.model.TimeOff;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Body of POST and PUT /businesses/{businessId}/staff/{staffId}/time-off. starts_at and ends_at are
 * "yyyy-MM-ddTHH:mm" on the salon's clock; a whole day off runs from 00:00 to 00:00 the next day.
 * PUT replaces every field.
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
