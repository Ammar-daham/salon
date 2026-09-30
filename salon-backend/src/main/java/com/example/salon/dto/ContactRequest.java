package com.example.salon.dto;

import com.example.salon.model.Contact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Shape for both a nested contact on user/business creation and a standalone contact update. */
public record ContactRequest(
		@NotBlank @Size(max = 20) String type,
		@NotBlank @Size(max = 255) String value)
{
	public Contact toContact()
	{
		return new Contact(null, type, value, null, null);
	}
}
