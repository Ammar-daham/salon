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
 * A new user, as {@code POST /api/v1/users} takes it. {@code first_name} and {@code last_name}, up to 100
 * characters each, and {@code role} are required. {@code email} and {@code password} are required for
 * every role but CUSTOMER; UserService.addUser checks that, since it depends on the role. Whichever is
 * sent must be well formed: {@code email} an email address, {@code password} as {@link Password} says: 8 to
 * 72 characters, and at most 72 bytes in UTF-8, BCrypt's input limit. {@code business_id} is the user's
 * salon: required when a SUPER_ADMIN creates the user, unless the user is a SUPER_ADMIN, who may have none,
 * and ignored for an ADMIN, whose own salon is used. {@code addresses} ({@link AddressRequest}) and
 * {@code contacts} ({@link ContactRequest}) are optional, and created with the user.
 */
public record CreateUserRequest(
		@NotBlank @Size(max = 100) @JsonProperty("first_name") String firstName,
		@NotBlank @Size(max = 100) @JsonProperty("last_name") String lastName,
		@NotNull Role role,
		@Email String email,
		@Password String password,
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
