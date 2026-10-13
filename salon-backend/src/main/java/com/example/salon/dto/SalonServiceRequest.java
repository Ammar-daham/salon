package com.example.salon.dto;

import com.example.salon.model.SalonService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * The body of {@code POST /api/v1/businesses/{businessId}/services} and of {@code PUT} on one service: a
 * service on the salon's menu.
 *
 * <p>{@code name} is required, at most 100 characters; {@code description} is optional.
 * {@code duration_minutes} is required, a whole number of at least 1. {@code price} is required, in the
 * salon's {@code currency}: 0 or more, with at most 8 digits before the point and 2 after.
 * {@code is_active} says whether the service can be booked. A {@code PUT} replaces every field.
 */
public record SalonServiceRequest(
		@NotBlank @Size(max = 100) String name,
		String description,
		@NotNull @Positive @JsonProperty("duration_minutes") Integer duration,
		@NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
		@JsonProperty("is_active") boolean active)
{
	public SalonService toSalonService()
	{
		return new SalonService(null, name, description, duration, price, active, null, null);
	}
}
