package com.example.salon.dto;

import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * email/password aren't @NotBlank here: UserService.addUser only requires them for a role that can
 * log in (everyone but CUSTOMER), a decision this shape can't make without the role. What this DOES
 * enforce up front, for whichever value is actually supplied: a well-formed email, and a password
 * long enough to be worth hashing. 72 is BCryptPasswordEncoder's input limit - bytes beyond it are
 * silently ignored, so a longer password would be misleadingly "accepted" without adding any strength.
 */
public record CreateUserRequest(
		@NotBlank @Size(max = 100) @JsonProperty("first_name") String firstName,
		@NotBlank @Size(max = 100) @JsonProperty("last_name") String lastName,
		@NotNull Role role,
		@Email String email,
		@Size(min = 8, max = 72, message = "must be at least 8 characters") String password,
		@JsonProperty("business_id") Long businessId,
		@Valid List<AddressRequest> addresses,
		@Valid List<ContactRequest> contacts)
{
	public User toUser()
	{
		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setRole(role);
		user.setEmail(email);
		user.setPassword(password);
		user.setBusinessId(businessId);
		if (addresses != null)
			user.setAddresses(addresses.stream().map(AddressRequest::toAddress).toList());
		if (contacts != null)
			user.setContacts(contacts.stream().map(ContactRequest::toContact).toList());
		return user;
	}
}
