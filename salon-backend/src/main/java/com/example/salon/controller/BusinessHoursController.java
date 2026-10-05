package com.example.salon.controller;

import com.example.salon.dto.BusinessHoursRequest;
import com.example.salon.model.BusinessHours;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.BusinessHoursService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/businesses")
public class BusinessHoursController
{
	private final BusinessHoursService businessHoursService;

	@Autowired
	public BusinessHoursController(BusinessHoursService businessHoursService)
	{
		this.businessHoursService = businessHoursService;
	}

	@GetMapping("/{businessId}/hours")
	public BusinessHours getHours(@PathVariable long businessId, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return businessHoursService.getHours(businessId, principal);
	}

	/** Replaces the whole week and returns it as stored, in order. */
	@PutMapping("/{businessId}/hours")
	public BusinessHours replaceHours(@PathVariable long businessId, @Valid @RequestBody BusinessHoursRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return businessHoursService.replaceHours(businessId, request.toOpeningIntervals(), principal);
	}
}
