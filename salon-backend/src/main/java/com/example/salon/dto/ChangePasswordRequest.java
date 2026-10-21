package com.example.salon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * What {@code POST /api/v1/auth/change-password} takes: {@code current_password}, the user's password
 * now, and {@code new_password}, following {@link Password}: 8 to 72 characters, and at most 72 bytes in
 * UTF-8. Both are required.
 */
public record ChangePasswordRequest(
		@NotBlank @JsonProperty("current_password") String currentPassword,
		@NotBlank @Password @JsonProperty("new_password") String newPassword)
{
}
