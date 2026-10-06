package com.example.salon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Body of PUT /businesses/{businessId}/staff/{staffId}/services: every service this staff member performs,
 * replacing the stored set. Each must be one of the salon's own services; repeats are ignored.
 */
public record StaffServicesRequest(@NotNull @JsonProperty("service_ids") List<@NotNull Long> serviceIds)
{
}
