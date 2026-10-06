package com.example.salon.model;

import java.time.LocalDateTime;

/** What a booking asks for: startsAt is on the salon's clock, and the service sets the length and the price. */
public record Booking(long customerId, long staffId, long serviceId, LocalDateTime startsAt, String notes)
{
}
