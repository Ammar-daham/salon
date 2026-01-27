package com.example.salon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class Address {

    public Long id;
    public String country;
    public String city;
    public String street;
    public String postalCode;
    public String latitude;
    public String longitude;
    public Instant createdAt;
    public Instant updatedAt;
    @JsonIgnore
    public Long businessId;
    public Long userId;

    public Address(@JsonProperty("id") Long id,
                   @JsonProperty("country") String country,
                   @JsonProperty("city") String city,
                   @JsonProperty("street") String street,
                   @JsonProperty("postal_code") String postalCode,
                   @JsonProperty("latitude") String latitude,
                   @JsonProperty("longitude") String longitude,
                   @JsonProperty("created_at") Instant createdAt,
                   @JsonProperty("updated_at") Instant updatedAt
    ) {
        this.id = id;
        this.country = country;
        this.city = city;
        this.street = street;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getStreet() {
        return street;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
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

    public void setCountry(String country) {
        this.country = country;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
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
