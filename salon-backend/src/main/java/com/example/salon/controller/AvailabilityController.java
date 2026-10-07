package com.example.salon.controller;

import com.example.salon.model.Availability;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.AvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
	 * from and to are "yyyy-MM-dd" on the salon's clock, both included: from defaults to today, to to from.
	 * staff_id narrows it to one staff member.
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
