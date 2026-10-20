package com.example.salon.dto;

import com.example.salon.model.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PUT /api/v1/users/{id}/role}: the {@code role} to give the user. It is required and must be
 * one of the {@link Role} names, such as {@code EMPLOYEE}.
 */
public record UserRoleRequest(@NotNull Role role)
{
}
