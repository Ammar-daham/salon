package com.example.salon.dto;

import com.example.salon.model.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Shape for both a nested address on user/business creation and a standalone address update. */
public record AddressRequest(
		@NotBlank @Size(max = 255) String street,
		@NotBlank @Size(max = 100) String city,
		@NotBlank @Size(max = 100) String country,
		@JsonProperty("postal_code") @Size(max = 20) String postalCode,
		@Size(max = 255) String latitude,
		@Size(max = 255) String longitude)
{
	public Address toAddress()
	{
		return new Address(null, country, city, street, postalCode, latitude, longitude, null, null);
	}
}
