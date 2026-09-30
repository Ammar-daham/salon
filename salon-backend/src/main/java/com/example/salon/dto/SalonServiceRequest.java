package com.example.salon.dto;

import com.example.salon.model.SalonService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Same shape for creating and updating a service - both write every column. */
public record SalonServiceRequest(
		@NotBlank @Size(max = 100) String name,
		String description,
		@NotNull @Positive @JsonProperty("duration_minutes") Integer duration,
		@PositiveOrZero double price,
		@JsonProperty("is_active") boolean active)
{
	public SalonService toSalonService()
	{
		return new SalonService(null, name, description, duration, price, active, null, null);
	}
}
