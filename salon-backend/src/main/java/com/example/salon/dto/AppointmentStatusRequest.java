package com.example.salon.dto;

import com.example.salon.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

/** Body of PUT /businesses/{businessId}/appointments/{appointmentId}/status. */
public record AppointmentStatusRequest(@NotNull AppointmentStatus status)
{
}
