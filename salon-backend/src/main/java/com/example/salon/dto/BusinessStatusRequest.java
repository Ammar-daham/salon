package com.example.salon.dto;

import com.example.salon.model.Status;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PUT /api/v1/businesses/{id}/status}: the {@code status} to move the salon to. It is required and
 * must be one of the {@link Status} names, such as {@code APPROVED}.
 */
public record BusinessStatusRequest(@NotNull Status status)
{
}
