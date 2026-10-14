package com.example.salon.dto;

import com.example.salon.model.Staff;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * The body of {@code POST /api/v1/businesses/{businessId}/staff}: one of the salon's users taken on as
 * staff.
 *
 * <p>{@code user_id} is required: an EMPLOYEE or ADMIN of the salon with no current staff record.
 * {@code title} is required, at most 50 characters. {@code hired_at} is a required date such as
 * {@code 2024-01-15}. {@code is_active} says whether the person can be booked. {@code calendar_colour} is
 * optional, {@code #} and six hex digits such as {@code #FF5733}.
 */
public record CreateStaffRequest(
		@NotNull @JsonProperty("user_id") Long userId,
		@NotBlank @Size(max = 50) String title,
		@JsonProperty("is_active") boolean active,
		@NotNull @JsonProperty("hired_at") LocalDate hiredAt,
		@Pattern(regexp = "^#[0-9A-Fa-f]{6}$") @JsonProperty("calendar_colour") String calendarColour)
{
	public Staff toStaff()
	{
		return new Staff(null, title, active, userId, hiredAt, calendarColour, null, null);
	}
}
