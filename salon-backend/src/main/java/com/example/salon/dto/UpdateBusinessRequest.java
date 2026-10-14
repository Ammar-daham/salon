package com.example.salon.dto;

import com.example.salon.model.Business;
import com.example.salon.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The body of {@code PUT /api/v1/businesses/{id}}: a salon's new details.
 *
 * <p>{@code name} is required, at most 100 characters. {@code description} is replaced, so leaving it out
 * clears it. {@code image}, {@code status}, {@code currency} and {@code timezone} are optional on purpose:
 * left out, each keeps its stored value; sent, {@code currency} and {@code timezone} follow the rules of
 * {@link CreateBusinessRequest}. Only a SUPER_ADMIN may send a {@code status} other than the current one;
 * that is checked against the caller, not here. Addresses and contacts aren't part of it.
 */
public record UpdateBusinessRequest(
		@NotBlank @Size(max = 100) String name,
		String description,
		String image,
		Status status,
		@Pattern(regexp = "^[A-Z]{3}$") String currency,
		@TimeZoneId String timezone)
{
	public Business toBusiness()
	{
		Business business = new Business(name, description);
		business.setImage(image);
		business.setStatus(status);
		business.setCurrency(currency);
		business.setTimezone(timezone);
		return business;
	}
}
