package com.example.salon.dto;

import com.example.salon.model.Business;
import com.example.salon.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * description/image/status are nullable on purpose: BusinessDataAccessService.updateBusinessById
 * COALESCEs image and status, so omitting them keeps the stored value. Whether a non-null status is
 * actually honoured is still gated by caller role in BusinessService, not by this shape.
 */
public record UpdateBusinessRequest(
		@NotBlank @Size(max = 100) String name,
		String description,
		String image,
		Status status)
{
	public Business toBusiness()
	{
		Business business = new Business(name, description);
		business.setImage(image);
		business.setStatus(status);
		return business;
	}
}
