package com.example.salon.security;

/**
 * What {@code POST /api/v1/auth/login} takes: {@code email} and {@code password}. Neither is checked for
 * shape; a missing one gets 401, like a wrong one. The email's case doesn't matter.
 */
public record LoginRequest(String email, String password)
{
}
