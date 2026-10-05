package com.example.salon.dto;

import com.example.salon.model.Customer;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST and PUT /businesses/{businessId}/customers. PUT replaces every field. */
public record CustomerRequest(
		@NotBlank @Size(max = 100) @JsonProperty("first_name") String firstName,
		@NotBlank @Size(max = 100) @JsonProperty("last_name") String lastName,
		@Email @Size(max = 255) String email,
		@Size(max = 50) String phone,
		@Size(max = 5000) String notes,
		@JsonProperty("marketing_consent") boolean marketingConsent)
{
	public Customer toCustomer()
	{
		return new Customer(null, null, null, firstName, lastName, blankToNull(email), blankToNull(phone),
				blankToNull(notes), marketingConsent, null, null);
	}

	private static String blankToNull(String value)
	{
		return value == null || value.isBlank() ? null : value.trim();
	}
}
