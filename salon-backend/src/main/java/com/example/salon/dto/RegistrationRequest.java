package com.example.salon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * What {@code POST /api/v1/auth/register} takes: the {@code owner} ({@link OwnerRequest}) and their
 * {@code salon}, with the same details, addresses and contacts as {@link CreateBusinessRequest}. Both are
 * required. A {@code status} in the salon is ignored: a registered salon always starts PENDING.
 */
public record RegistrationRequest(
		@Valid @NotNull OwnerRequest owner,
		@Valid @NotNull CreateBusinessRequest salon)
{
}
