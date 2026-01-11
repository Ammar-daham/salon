package com.example.salon.model;

import java.time.Instant;

public class Contact {

    public Long id;
    public String type;
    public String value;
    public Instant createdAt;
    public Long businessId;

    public Contact(Long id, String type, String value, Instant createdAt, Long businessId) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.createdAt = createdAt;
        this.businessId = businessId;
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

    public Long getBusinessId() {
        return businessId;
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

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }
}
