package com.example.salon.dto;

import com.example.salon.model.Contact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A contact, as {@code PUT /api/v1/contacts/{id}} takes it, and in {@code contacts} on
 * {@code POST /api/v1/users} and {@code POST /api/v1/businesses}: {@code type}, such as {@code phone}, up
 * to 20 characters, and {@code value}, up to 255; both required. One owner can't have the same
 * {@code value} twice.
 */
public record ContactRequest(
		@NotBlank @Size(max = 20) String type,
		@NotBlank @Size(max = 255) String value)
{
	public Contact toContact()
	{
		return new Contact(null, type, value, null, null);
	}
}
