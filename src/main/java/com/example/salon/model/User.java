package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.List;

public class User {
    public Long id;
    public String firstName;
    public String lastName;
    public Role role;
    @JsonIgnore
    public Long businessId;
    public List<Address> addresses;
    public List<Contact> contacts;
    public Instant createdAt;

    public User(Long id, String firstName, String lastName, Role role, List<Address> addresses, List<Contact> contacts, Instant createdAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.addresses = addresses;
        this.contacts = contacts;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Role getRole() {
        return role;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
