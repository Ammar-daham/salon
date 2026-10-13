package com.example.salon.controller;

import com.example.salon.model.Availability;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.AvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * When a service can be booked, under {@code /api/v1/businesses/{businessId}/services/{serviceId}/availability}.
 *
 * <p>Only the salon's ADMIN and EMPLOYEE users and a SUPER_ADMIN may call it, since the gaps show when people are
 * away. It is advice for the booking form: booking itself only refuses a double-booking, so staff can still fit
 * someone in outside these slots.
 */
@RestController
@RequestMapping("api/v1/businesses/{businessId}/services/{serviceId}/availability")
public class AvailabilityController
{
	private final AvailabilityService availabilityService;

	@Autowired
	public AvailabilityController(AvailabilityService availabilityService)
	{
		this.availabilityService = availabilityService;
	}

	/**
	 * Lists the open slots for a service, per active staff member who performs it.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/services/{serviceId}/availability}. ADMIN and EMPLOYEE of the
	 * salon, or a SUPER_ADMIN. A slot lasts the service's duration and starts on a quarter hour of the salon's
	 * clock. It lies inside the salon's opening hours and the staff member's shift that weekday, is clear of their
	 * time off and of appointments that aren't cancelled or missed, and hasn't started yet. The days run from
	 * {@code from} to {@code to}, both included and at most 31 of them. {@code staff_id} narrows it to one staff
	 * member, who must be active and perform the service. A staff member with no open slots is still listed.
	 *
	 * <p>Errors: 400 for a malformed date, {@code to} before {@code from}, more than 31 days, an inactive service,
	 * or a {@code staff_id} who is inactive or doesn't perform the service; 403 for another salon; 404 if the salon
	 * doesn't exist or the service isn't one of its current ones.
	 *
	 * @param businessId the salon
	 * @param serviceId the service to book
	 * @param from the first day, {@code yyyy-MM-dd}, or nothing for today on the salon's clock
	 * @param to the last day, {@code yyyy-MM-dd}, or nothing for the same day as {@code from}
	 * @param staffId only this staff member, or nothing for every active one who performs the service
	 * @param principal the signed-in caller
	 * @return 200 with the salon's {@code timezone}, the service's {@code duration_minutes} and each staff member's
	 * slots: see {@link Availability}
	 */
	@GetMapping
	public Availability getAvailability(@PathVariable long businessId, @PathVariable long serviceId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(name = "staff_id", required = false) Long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return availabilityService.getAvailability(businessId, serviceId, from, to, staffId, principal);
	}
}
