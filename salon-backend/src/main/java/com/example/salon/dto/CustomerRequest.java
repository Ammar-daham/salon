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
 * <p>Every text field is trimmed first, and the rules apply to what's left. {@code first_name} and
 * {@code last_name} are required, up to 100 characters each. {@code email} (an email address, up to 255),
 * {@code phone} (up to 50) and {@code notes} (up to 5000) are optional; a blank one counts as left out. Anything
 * else in the body, such as {@code id} or {@code business_id}, is ignored.
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
	/** Trims before validation runs, which checks the fields this sets, so a padded email or name isn't refused. */
	public CustomerRequest
	{
		firstName = firstName == null ? null : firstName.trim();
		lastName = lastName == null ? null : lastName.trim();
		email = blankToNull(email);
		phone = blankToNull(phone);
		notes = blankToNull(notes);
	}

	public Customer toCustomer()
	{
		return new Customer(null, null, null, firstName, lastName, email, phone, notes,
				Boolean.TRUE.equals(marketingConsent), null, null);
	}

	private static String blankToNull(String value)
	{
		return value == null || value.isBlank() ? null : value.trim();
	}
}
