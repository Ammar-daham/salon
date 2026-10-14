package com.example.salon.model;

/** What a user may do: their {@code role}. */
public enum Role {
    /** A salon's client with an account. Does no salon work. */
    CUSTOMER,
    /** Works at one salon, their {@code business_id}. */
    EMPLOYEE,
    /** Runs one salon, their {@code business_id}. */
    ADMIN,
    /** Runs the whole platform: every salon and every user. */
    SUPER_ADMIN
}
