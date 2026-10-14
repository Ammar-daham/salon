package com.example.salon.dto;

import com.example.salon.model.Staff;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * The body of {@code PUT /api/v1/businesses/{businessId}/staff/{staffId}}: a staff record's new details.
 *
 * <p>The fields of {@link CreateStaffRequest} but {@code user_id}, under the same rules: who the record is
 * for can't change. Every field is replaced, so leaving {@code calendar_colour} out clears it.
 */
public record UpdateStaffRequest(
		@NotBlank @Size(max = 50) String title,
		@JsonProperty("is_active") boolean active,
		@NotNull @JsonProperty("hired_at") LocalDate hiredAt,
		@Pattern(regexp = "^#[0-9A-Fa-f]{6}$") @JsonProperty("calendar_colour") String calendarColour)
{
	public Staff toStaff()
	{
		return new Staff(null, title, active, null, hiredAt, calendarColour, null, null);
	}
}
