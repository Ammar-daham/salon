package com.example.salon.dto;

import com.example.salon.model.Business;
import com.example.salon.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * description/image/status/currency/timezone are nullable on purpose: BusinessDataAccessService.updateBusinessById
 * COALESCEs image, status, currency and timezone, so omitting them keeps the stored value. Whether a non-null status is
 * actually honoured is still gated by caller role in BusinessService, not by this shape.
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
