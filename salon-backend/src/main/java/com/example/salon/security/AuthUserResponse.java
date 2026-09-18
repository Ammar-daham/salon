package com.example.salon.security;

import com.example.salon.model.Role;
import com.example.salon.model.User;

public record AuthUserResponse(Long id, String firstName, String lastName, String email, Role role)
{
	public static AuthUserResponse from(User user)
	{
		return new AuthUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole());
	}
}
