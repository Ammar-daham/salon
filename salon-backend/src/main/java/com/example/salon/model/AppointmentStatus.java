package com.example.salon.model;

/**
 * Where an appointment is in BOOKED → CONFIRMED → COMPLETED / CANCELLED / NO_SHOW. Confirming is optional, and
 * the last three are final.
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
