package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public  class Business {

    public Long id;
    public String name;
    public  String description;
    public Instant createdAt;


    public Business(@JsonProperty("id") Long id, @JsonProperty("name") String name,
                    @JsonProperty("description") String description,  @JsonProperty("created_at") Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt; // ⏱ timestamp created automatically
    }

    @JsonCreator
    public Business(@JsonProperty("name") String name,
                    @JsonProperty("description") String description) {
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now(); // ⏱ timestamp created automatically
    }


    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
