package com.example.salon.dto;

import com.example.salon.model.Booking;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Body of {@code POST /api/v1/businesses/{businessId}/appointments} and
 * {@code PUT /api/v1/businesses/{businessId}/appointments/{appointmentId}}: who, what and when. PUT replaces every
 * field.
 *
 * <p>{@code customer_id}, {@code staff_id} and {@code service_id} are required, and name the salon's own customer,
 * staff member and service. {@code starts_at} is required, as {@code yyyy-MM-dd'T'HH:mm} on the salon's clock; a
 * time that a daylight-saving change skips moves forward by the gap, and one it repeats takes the earlier offset.
 * {@code notes} is optional, up to 5000 characters, and trimmed; a blank one counts as left out. The service sets
 * how long the appointment lasts and what it costs, so {@code ends_at}, {@code price}, {@code status} or anything
 * else sent is ignored.
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
