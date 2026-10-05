package com.example.salon.controller;

import com.example.salon.dto.StaffScheduleRequest;
import com.example.salon.model.StaffSchedule;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.StaffScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/businesses/{businessId}/staff/{staffId}")
public class StaffScheduleController
{
	private final StaffScheduleService staffScheduleService;

	@Autowired
	public StaffScheduleController(StaffScheduleService staffScheduleService)
	{
		this.staffScheduleService = staffScheduleService;
	}

	@GetMapping("/schedule")
	public StaffSchedule getSchedule(@PathVariable long businessId, @PathVariable long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffScheduleService.getSchedule(businessId, staffId, principal);
	}

	/** Replaces the whole week and returns it as stored, in order. */
	@PutMapping("/schedule")
	public StaffSchedule replaceSchedule(@PathVariable long businessId, @PathVariable long staffId,
			@Valid @RequestBody StaffScheduleRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffScheduleService.replaceSchedule(businessId, staffId, request.toWorkingIntervals(), principal);
	}
}
