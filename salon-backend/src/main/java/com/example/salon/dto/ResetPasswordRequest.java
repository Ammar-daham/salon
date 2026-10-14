package com.example.salon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What {@code POST /api/v1/auth/reset-password} takes: {@code token}, from the reset link, and
 * {@code password}, the new one, 8 to 72 characters as for {@link CreateUserRequest}. Both are required.
 */
public record ResetPasswordRequest(
		@NotBlank String token,
		@NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password)
{
}
