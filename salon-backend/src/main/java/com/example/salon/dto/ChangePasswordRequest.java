package com.example.salon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The new password follows CreateUserRequest's rule: 72 is BCrypt's input limit. */
public record ChangePasswordRequest(
		@NotBlank @JsonProperty("current_password") String currentPassword,
		@NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
		@JsonProperty("new_password") String newPassword)
{
}
