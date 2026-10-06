package com.example.salon.model;

/** Where an appointment is in BOOKED → CONFIRMED → COMPLETED / CANCELLED / NO_SHOW. */
public enum AppointmentStatus {
    BOOKED,
    CONFIRMED,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
