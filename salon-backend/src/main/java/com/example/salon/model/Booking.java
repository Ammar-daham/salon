package com.example.salon.model;

import java.time.LocalDateTime;

/**
 * What a booking or an edit of one asks for, made from an {@code AppointmentRequest}: startsAt is on the salon's
 * clock, the service sets the length and the price, and notes is null when left blank.
 */
public record Booking(long customerId, long staffId, long serviceId, LocalDateTime startsAt, String notes)
{
}
