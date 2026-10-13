package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * One service by one staff member for one customer, as every {@code /api/v1/businesses/{businessId}/appointments}
 * endpoint returns it.
 *
 * <p>{@code customer} and {@code staff} carry an {@code id}, {@code first_name} and {@code last_name}, and
 * {@code service} an {@code id} and {@code name}. They keep their names even once the customer, staff member or
 * service is removed, so a salon's history stays readable. Like time off, the database stores two instants and
 * the API reads them as {@code starts_at} and {@code ends_at}, {@code yyyy-MM-dd'T'HH:mm} on the salon's clock.
 * The length and {@code price} come from the service when it is booked, or when an edit changes the service;
 * later changes to the service leave them alone. {@code price} is in the salon's currency. {@code status} is an
 * {@link AppointmentStatus}, and {@code updated_at} is null until the appointment is first changed.
 */
public record Appointment(
        Long id,
        PersonRef customer,
        PersonRef staff,
        ServiceRef service,
        @JsonProperty("starts_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startsAt,
        @JsonProperty("ends_at") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endsAt,
        AppointmentStatus status,
        BigDecimal price,
        String notes,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt)
{
    public record PersonRef(
            Long id,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName)
    {
    }

    public record ServiceRef(Long id, String name)
    {
    }
}
