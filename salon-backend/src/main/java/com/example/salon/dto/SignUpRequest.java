package com.example.salon.dto;

import com.example.salon.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What {@code POST /api/v1/auth/sign-up} takes: the client's {@code first_name} and {@code last_name}, up to
 * 100 characters each, and the {@code email} and {@code password} they'll sign in with, the password as
 * {@link Password} says. All are required. Anything else, such as a {@code role} or {@code business_id}, is
 * ignored: the account is always a CUSTOMER's, with no salon.
 */
public record SignUpRequest(
		@NotBlank @Size(max = 100) @JsonProperty("first_name") String firstName,
		@NotBlank @Size(max = 100) @JsonProperty("last_name") String lastName,
		@NotBlank @Email String email,
		@NotBlank @Password String password)
{
	public User toUser()
	{
		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setEmail(email);
		user.setPassword(password);
		return user;
	}
}
