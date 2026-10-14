package com.example.salon.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** What {@code POST /api/v1/auth/forgot-password} takes: {@code email}, required, an email address. */
public record ForgotPasswordRequest(@NotBlank @Email String email)
{
}
