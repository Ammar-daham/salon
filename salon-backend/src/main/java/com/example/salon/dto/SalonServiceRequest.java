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

/** price is in the salon's currency (businesses.currency) and must fit services.price NUMERIC(10,2). */
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
