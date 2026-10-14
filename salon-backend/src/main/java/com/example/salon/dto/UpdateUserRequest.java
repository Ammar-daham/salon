package com.example.salon.dto;

import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A user's new name and role, as {@code PUT /api/v1/users/{id}} takes it: {@code first_name} and
 * {@code last_name}, up to 100 characters each, and {@code role}, all required. The role only changes
 * when an ADMIN or SUPER_ADMIN sends it. The email, password and salon can't be changed this way.
 */
public record UpdateUserRequest(
		@NotBlank @Size(max = 100) @JsonProperty("first_name") String firstName,
		@NotBlank @Size(max = 100) @JsonProperty("last_name") String lastName,
		@NotNull Role role)
{
	public User toUser()
	{
		User user = new User();
		user.setFirstName(firstName);
		user.setLastName(lastName);
		user.setRole(role);
		return user;
	}
}
