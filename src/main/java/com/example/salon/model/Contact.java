package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public class Contact {

    public Long id;
    public String type;
    public String value;
    public Instant createdAt;
    public Instant updatedAt;
    @JsonIgnore
    public Long businessId;
    @JsonIgnore
    public Long userId;

    public Contact(Long id, String type, String value, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
