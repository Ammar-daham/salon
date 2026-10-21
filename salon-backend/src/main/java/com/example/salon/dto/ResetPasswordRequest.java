package com.example.salon.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * What {@code POST /api/v1/auth/reset-password} takes: {@code token}, from the reset link, and
 * {@code password}, the new one, following {@link Password}: 8 to 72 characters, and at most 72 bytes in
 * UTF-8. Both are required.
 */
public record ResetPasswordRequest(
		@NotBlank String token,
		@NotBlank @Password String password)
{
}
