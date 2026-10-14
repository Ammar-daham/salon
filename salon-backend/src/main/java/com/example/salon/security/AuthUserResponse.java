package com.example.salon.security;

import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The signed-in user, as {@code POST /api/v1/auth/login} and {@code GET /api/v1/auth/me} answer:
 * {@code id}, {@code first_name}, {@code last_name}, {@code email}, {@code role} and {@code business_id}.
 * {@code business_id} is the user's salon; the platform's SUPER_ADMIN normally has none. User records
 * leave it out, so this is where a client learns it.
 */
public record AuthUserResponse(
		Long id,
		@JsonProperty("first_name") String firstName,
		@JsonProperty("last_name") String lastName,
		String email,
		Role role,
		@JsonProperty("business_id") Long businessId)
{
	public static AuthUserResponse from(User user)
	{
		// businessId is normally null for SUPER_ADMIN (platform scope) and is already loaded by
		// UserDataAccessService.userRowMapper(). User.businessId is WRITE_ONLY, so this
		// response is the only place the client can learn which business it belongs to.
		return new AuthUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(), user.getBusinessId());
	}
}
