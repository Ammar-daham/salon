package com.example.salon.dto;

import com.example.salon.model.Booking;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Body of POST and PUT /businesses/{businessId}/appointments. starts_at is "yyyy-MM-ddTHH:mm" on the salon's
 * clock; the service sets how long the appointment lasts and what it costs. PUT replaces every field.
 */
public record AppointmentRequest(
		@NotNull @JsonProperty("customer_id") Long customerId,
		@NotNull @JsonProperty("staff_id") Long staffId,
		@NotNull @JsonProperty("service_id") Long serviceId,
		@NotNull @JsonProperty("starts_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startsAt,
		@Size(max = 5000) String notes)
{
	public Booking toBooking()
	{
		return new Booking(customerId, staffId, serviceId, startsAt,
				notes == null || notes.isBlank() ? null : notes.trim());
	}
}
