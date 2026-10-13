package com.example.salon.dto;

import com.example.salon.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PUT /api/v1/businesses/{businessId}/appointments/{appointmentId}/status}: the {@code status} to
 * move to. It is required and must be one of the {@link AppointmentStatus} names, such as {@code CANCELLED}.
 */
public record AppointmentStatusRequest(@NotNull AppointmentStatus status)
{
}
