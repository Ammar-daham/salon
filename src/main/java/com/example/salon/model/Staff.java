package com.example.salon.model;

import java.time.Instant;

public class Staff {
    public long id;
    public String title;
    public boolean isActive;
    public long userId;
    public long businessId;
    public Instant createdAt;
    public Instant updatedAt;

    public Staff(long id, String title, boolean isActive, long userId, long businessId) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isActive() {
        return isActive;
    }

    public long getUserId() {
        return userId;
    }

    public long getBusinessId() {
        return businessId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setBusinessId(long businessId) {
        this.businessId = businessId;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
