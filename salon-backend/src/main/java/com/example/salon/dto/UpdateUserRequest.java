package com.example.salon.dto;

import com.example.salon.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A user's new name, as {@code PUT /api/v1/users/{id}} takes it: {@code first_name} and {@code last_name},
 * up to 100 characters each, both required. The role changes with {@code PUT /api/v1/users/{id}/role}, and
 * the email, password and salon can't be changed this way.
 */
public record UpdateUserRequest(
		@NotBlank @Size(max = 100) @JsonProperty("first_name") String firstName,
		@NotBlank @Size(max = 100) @JsonProperty("last_name") String lastName)
{
	public User toUser()
	{
		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		return user;
	}
}
