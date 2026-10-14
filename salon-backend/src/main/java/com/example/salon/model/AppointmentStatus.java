package com.example.salon.model;

/**
 * Where an appointment is in {@code BOOKED} &rarr; {@code CONFIRMED} &rarr; {@code COMPLETED}, {@code CANCELLED}
 * or {@code NO_SHOW}, as {@code status} on the wire.
 *
 * <p>A new appointment is {@code BOOKED}. That can become any of the other four, so confirming is optional;
 * {@code CONFIRMED} can become any of the last three, but not {@code BOOKED} again; and the last three are final.
 * Only a {@code BOOKED} or {@code CONFIRMED} appointment can be edited. {@code COMPLETED} and {@code NO_SHOW} are
 * refused before the appointment starts, and a {@code CANCELLED} or {@code NO_SHOW} one frees the staff member's
 * time for another booking.
 */
public enum AppointmentStatus {
    BOOKED,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    NO_SHOW;

    /** Booked or confirmed: it hasn't ended one way or another, so it can still be changed. */
    public boolean isOpen()
    {
        return this == BOOKED || this == CONFIRMED;
    }

    public boolean canBecome(AppointmentStatus next)
    {
        return switch (this) {
            case BOOKED -> next != BOOKED;
            case CONFIRMED -> !next.isOpen();
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
    }
}
