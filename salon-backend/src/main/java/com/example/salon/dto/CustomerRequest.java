package com.example.salon.dto;

import com.example.salon.model.Customer;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/v1/businesses/{businessId}/customers} and
 * {@code PUT /api/v1/businesses/{businessId}/customers/{customerId}}: a customer's details. PUT replaces every
 * field, so one left out is cleared.
 *
 * <p>{@code first_name} and {@code last_name} are required, up to 100 characters each. {@code email} (an email
 * address, up to 255), {@code phone} (up to 50) and {@code notes} (up to 5000) are optional; an empty one counts
 * as left out, and phone and notes are trimmed. Anything else in the body, such as {@code id} or
 * {@code business_id}, is ignored.
 *
 * <p>{@code marketing_consent} is a Boolean so it can be left out: consent has to be given explicitly, so a
 * missing value means no. (Jackson 3 rejects a missing primitive boolean as malformed JSON.)
 */
public record CustomerRequest(
		@NotBlank @Size(max = 100) @JsonProperty("first_name") String firstName,
		@NotBlank @Size(max = 100) @JsonProperty("last_name") String lastName,
		@Email @Size(max = 255) String email,
		@Size(max = 50) String phone,
		@Size(max = 5000) String notes,
		@JsonProperty("marketing_consent") Boolean marketingConsent)
{
	public Customer toCustomer()
	{
		return new Customer(null, null, null, firstName, lastName, blankToNull(email), blankToNull(phone),
				blankToNull(notes), Boolean.TRUE.equals(marketingConsent), null, null);
	}

	private static String blankToNull(String value)
	{
		return value == null || value.isBlank() ? null : value.trim();
	}
}
