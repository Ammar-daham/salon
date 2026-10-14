package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A service on a salon's menu, as {@code GET /api/v1/businesses/{businessId}/services/{serviceId}} and the
 * staff member's services endpoints return it, and as it appears in a salon's {@code services}.
 *
 * <p>{@code duration_minutes} is how long it takes, {@code price} what it costs in the salon's
 * {@code currency}, and {@code is_active} whether it can be booked. {@code updated_at} is null until the
 * service is first changed.
 */
public class SalonService {
    public Long id;
    public String name;
    public String description;
    public Integer duration;
    public BigDecimal price;
    public boolean active;
    public Instant createdAt;
    public Instant updatedAt;

    public SalonService(
            @JsonProperty("id") Long id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("duration_minutes") Integer duration,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("is_active") boolean active,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.price = price;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDuration() {
        return duration;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
