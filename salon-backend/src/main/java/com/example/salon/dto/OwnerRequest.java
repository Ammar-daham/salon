package com.example.salon.dto;

import com.example.salon.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The person registering a salon, as {@code owner} in {@link RegistrationRequest}: their {@code first_name}
 * and {@code last_name}, up to 100 characters each, and the {@code email} and {@code password} they'll sign
 * in with, the password as {@link Password} says. All are required. They become the salon's ADMIN.
 */
public record OwnerRequest(
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
