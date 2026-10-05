package com.example.salon.dto;

import com.example.salon.model.Business;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateBusinessRequest(
		@NotBlank @Size(max = 100) String name,
		String description,
		@NotBlank String image,
		// ISO 4217, e.g. "EUR". Omitted means EUR.
		@Pattern(regexp = "^[A-Z]{3}$") String currency,
		// Where the salon is, e.g. "Europe/Berlin": its opening hours are on this clock. Omitted means Europe/Berlin.
		@TimeZoneId String timezone,
		@Valid List<AddressRequest> addresses,
		@Valid List<ContactRequest> contacts)
{
	public Business toBusiness()
	{
		Business business = new Business(name, description);
		business.setImage(image);
		business.setCurrency(currency);
		business.setTimezone(timezone);
		if (addresses != null)
			business.setAddresses(addresses.stream().map(AddressRequest::toAddress).toList());
		if (contacts != null)
			business.setContacts(contacts.stream().map(ContactRequest::toContact).toList());
		return business;
	}
}
