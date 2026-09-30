package com.example.salon.dto;

import com.example.salon.model.Business;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * status is deliberately absent: a newly created business is always PENDING (BusinessService), and
 * a client that could set it here could self-approve, bypassing platform moderation entirely.
 */
public record CreateBusinessRequest(
		@NotBlank @Size(max = 100) String name,
		String description,
		@NotBlank String image,
		@Valid List<AddressRequest> addresses,
		@Valid List<ContactRequest> contacts)
{
	public Business toBusiness()
	{
		Business business = new Business(name, description);
		business.setImage(image);
		if (addresses != null)
			business.setAddresses(addresses.stream().map(AddressRequest::toAddress).toList());
		if (contacts != null)
			business.setContacts(contacts.stream().map(ContactRequest::toContact).toList());
		return business;
	}
}
