package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * A salon's customer record, as {@code POST /api/v1/businesses/{businessId}/customers}, the customer lists and the
 * single reads under {@code /api/v1/businesses/{businessId}/customers} and {@code /api/v1/customers} return it.
 *
 * <p>{@code business_id} and {@code business_name} say whose client it is. {@code user_id} is the customer's own
 * account when one is linked, and null otherwise; the API doesn't set it. {@code email}, {@code phone} and
 * {@code notes} may be null. {@code updated_at} is null until the record is first changed.
 */
public class Customer
{
    public Long id;
    @JsonProperty("business_id")
    public Long businessId;
    // Read with the customer, so a list across salons says whose client each one is.
    @JsonProperty("business_name")
    public String businessName;
    @JsonProperty("user_id")
    public Long userId;
    @JsonProperty("first_name")
    public String firstName;
    @JsonProperty("last_name")
    public String lastName;
    public String email;
    public String phone;
    public String notes;
    @JsonProperty("marketing_consent")
    public boolean marketingConsent;
    @JsonProperty("created_at")
    public Instant createdAt;
    @JsonProperty("updated_at")
    public Instant updatedAt;

    public Customer(Long id, Long businessId, Long userId, String firstName, String lastName, String email,
            String phone, String notes, boolean marketingConsent, Instant createdAt, Instant updatedAt)
    {
        this.id = id;
        this.businessId = businessId;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.notes = notes;
        this.marketingConsent = marketingConsent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId()
    {
        return id;
    }

    public Long getBusinessId()
    {
        return businessId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public String getEmail()
    {
        return email;
    }

    public String getPhone()
    {
        return phone;
    }

    public String getNotes()
    {
        return notes;
    }

    public boolean isMarketingConsent()
    {
        return marketingConsent;
    }

    public Instant getCreatedAt()
    {
        return createdAt;
    }

    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getBusinessName()
    {
        return businessName;
    }

    public void setBusinessName(String businessName)
    {
        this.businessName = businessName;
    }

    public void setBusinessId(Long businessId)
    {
        this.businessId = businessId;
    }
}
