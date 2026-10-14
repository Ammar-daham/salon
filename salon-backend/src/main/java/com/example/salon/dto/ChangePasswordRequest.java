package com.example.salon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What {@code POST /api/v1/auth/change-password} takes: {@code current_password}, the user's password
 * now, and {@code new_password}, 8 to 72 characters as for {@link CreateUserRequest}. Both are required.
 */
public record ChangePasswordRequest(
		@NotBlank @JsonProperty("current_password") String currentPassword,
		@NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
		@JsonProperty("new_password") String newPassword)
{
}
