package com.example.salon.controller;

import com.example.salon.dto.StaffScheduleRequest;
import com.example.salon.dto.TimeOffRequest;
import com.example.salon.model.StaffSchedule;
import com.example.salon.model.TimeOff;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.StaffScheduleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

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

	@PostMapping("/time-off")
	public ResponseEntity<TimeOff> addTimeOff(@PathVariable long businessId, @PathVariable long staffId,
			@Valid @RequestBody TimeOffRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		TimeOff timeOff = staffScheduleService.addTimeOff(businessId, staffId, request.toTimeOff(), principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(timeOff.id())
				.toUri();

		return ResponseEntity.created(location).body(timeOff);
	}

	@GetMapping("/time-off")
	public List<TimeOff> getTimeOff(@PathVariable long businessId, @PathVariable long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffScheduleService.getTimeOff(businessId, staffId, principal);
	}

	@GetMapping("/time-off/{timeOffId}")
	public TimeOff getTimeOffById(@PathVariable long businessId, @PathVariable long staffId,
			@PathVariable long timeOffId, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffScheduleService.getTimeOffById(businessId, staffId, timeOffId, principal);
	}

	@PutMapping("/time-off/{timeOffId}")
	public void updateTimeOff(@PathVariable long businessId, @PathVariable long staffId, @PathVariable long timeOffId,
			@Valid @RequestBody TimeOffRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		staffScheduleService.updateTimeOff(businessId, staffId, timeOffId, request.toTimeOff(), principal);
	}

	@DeleteMapping("/time-off/{timeOffId}")
	public void deleteTimeOff(@PathVariable long businessId, @PathVariable long staffId, @PathVariable long timeOffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		staffScheduleService.deleteTimeOff(businessId, staffId, timeOffId, principal);
	}
}
