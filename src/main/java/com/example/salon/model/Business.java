package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public class Business {
    public Long id;
    public String name;
    public String description;
    public Instant createdAt;
    public Instant updatedAt;
    public String image;
    public List<Address> addresses;
    public List<Contact> contacts;


    public Business(@JsonProperty("id") Long id, @JsonProperty("name") String name,
                    @JsonProperty("description") String description, @JsonProperty("created_at") Instant createdAt,
                    @JsonProperty("image") String image) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.image = image;
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
}
