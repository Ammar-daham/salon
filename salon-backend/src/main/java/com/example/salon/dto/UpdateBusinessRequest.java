package com.example.salon.dto;

import com.example.salon.model.Business;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The body of {@code PUT /api/v1/businesses/{id}}: a salon's new details.
 *
 * <p>Every field is replaced, under the rules of {@link CreateBusinessRequest}. {@code name}, {@code image},
 * {@code currency} and {@code timezone} are required, and leaving {@code description} out clears it.
 * {@code status}, addresses and contacts aren't part of it: a salon's status changes with
 * {@code PUT /api/v1/businesses/{id}/status}.
 */
public record UpdateBusinessRequest(
		@NotBlank @Size(max = 100) String name,
		String description,
		@NotBlank String image,
		@NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency,
		@NotNull @TimeZoneId String timezone)
{
	public Business toBusiness()
	{
		Business business = new Business(name, description);
		business.setImage(image);
		business.setCurrency(currency);
		business.setTimezone(timezone);
		return business;
	}
}
