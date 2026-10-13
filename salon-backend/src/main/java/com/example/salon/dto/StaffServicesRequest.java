package com.example.salon.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * The body of {@code PUT /api/v1/businesses/{businessId}/staff/{staffId}/services}: every service this
 * staff member performs, replacing the stored set.
 *
 * <p>{@code service_ids} is required and may be empty. Each id must be one of the salon's own current
 * services; repeats are ignored.
 */
public record StaffServicesRequest(@NotNull @JsonProperty("service_ids") List<@NotNull Long> serviceIds)
{
}
