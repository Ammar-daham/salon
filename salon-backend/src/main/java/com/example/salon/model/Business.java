package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public class Business {
    public Long id;
    public String name;
    public String description;
    @JsonProperty("created_at")
    public Instant createdAt;
    @JsonProperty("updated_at")
    public Instant updatedAt;
    public String image;
    public Status status;
    public List<Address> addresses;
    public List<Contact> contacts;
    public List<SalonService> services;


    public Business(@JsonProperty("id") Long id, @JsonProperty("name") String name,
                    @JsonProperty("description") String description, @JsonProperty("created_at") Instant createdAt,
                    @JsonProperty("updated_at") Instant updatedAt, @JsonProperty("image") String image,
                    @JsonProperty("status") Status status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.image = image;
        this.status = status;
    }

    @JsonCreator
    public Business(@JsonProperty("name") String name,
                    @JsonProperty("description") String description) {
        this.name = name;
        this.description = description;
        this.createdAt = Instant.now();
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getImage() {
        return image;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public List<SalonService> getServices() {
        return services;
    }

    public Status getStatus() {
        return status;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public void setServices(List<SalonService> services) {
        this.services = services;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
