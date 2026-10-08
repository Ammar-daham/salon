package com.example.salon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The token from a reset link, and the new password, on CreateUserRequest's rule. */
public record ResetPasswordRequest(
		@NotBlank String token,
		@NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password)
{
}
