package com.example.salon.dto;

import com.example.salon.model.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * latitude/longitude are optional but come as a pair. They're stored as NUMERIC(9,6), so extra
 * decimals (a phone GPS often sends more) are rounded to 6 places, about 11 cm, rather than rejected.
 */
public record AddressRequest(
		@NotBlank @Size(max = 255) String street,
		@NotBlank @Size(max = 100) String city,
		@NotBlank @Size(max = 100) String country,
		@JsonProperty("postal_code") @Size(max = 20) String postalCode,
		@DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
		@DecimalMin("-180") @DecimalMax("180") BigDecimal longitude)
{
	@AssertTrue(message = "latitude and longitude must be given together")
	public boolean isCoordinatePair()
	{
		return (latitude == null) == (longitude == null);
	}

	public Address toAddress()
	{
		return new Address(null, country, city, street, postalCode, round(latitude), round(longitude), null, null);
	}

	private static BigDecimal round(BigDecimal coordinate)
	{
		return coordinate == null ? null : coordinate.setScale(6, RoundingMode.HALF_UP);
	}
}
