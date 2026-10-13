package com.example.salon.model;

/**
 * Where a salon stands with the platform, in its {@code status}. A new salon is {@code PENDING}, and only
 * a SUPER_ADMIN changes it, with {@code PUT /api/v1/businesses/{id}}. A salon that isn't {@code APPROVED}
 * is hidden from everyone but ADMIN and SUPER_ADMIN users and the salon's own users.
 */
public enum Status {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED
}
