package com.example.salon.dto;

import com.example.salon.model.Staff;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

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
