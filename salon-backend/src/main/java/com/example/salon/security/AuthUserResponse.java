package com.example.salon.security;

import com.example.salon.model.Role;
import com.example.salon.model.User;

public record AuthUserResponse(Long id, String firstName, String lastName, String email, Role role, Long businessId)
{
	public static AuthUserResponse from(User user)
	{
		// businessId is null for SUPER_ADMIN (platform scope) and is already loaded by
		// UserDataAccessService.userRowMapper(). User.businessId is WRITE_ONLY, so this
		// response is the only place the client can learn which business it belongs to.
		return new AuthUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole(), user.getBusinessId());
	}
}
