package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * One service by one staff member for one customer. Like TimeOff, the database stores two instants and the API
 * reads them as date-times on the salon's clock. customer, staff and service carry their names even once removed
 * (DB-13), so a salon's history stays readable. price is what the service cost when it was booked, in the
 * salon's currency.
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
