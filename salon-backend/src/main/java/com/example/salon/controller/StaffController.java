package com.example.salon.controller;

import com.example.salon.dto.CreateStaffRequest;
import com.example.salon.dto.StaffServicesRequest;
import com.example.salon.dto.UpdateStaffRequest;
import com.example.salon.model.SalonService;
import com.example.salon.model.Staff;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("api/v1/businesses")
public class StaffController
{
	private final StaffService staffService;

	@Autowired
	public StaffController(StaffService staffService)
	{
		this.staffService = staffService;
	}

	@PostMapping("/{businessId}/staff")
	public ResponseEntity<Staff> addStaffForBusiness(
			@PathVariable Long businessId,
			@Valid @RequestBody CreateStaffRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal
	)
	{
		Staff staff = request.toStaff();
		Staff s = staffService.addStaffForBusiness(businessId, staff, principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(staff.getId())
				.toUri();

		return ResponseEntity.created(location).body(s);
	}

	@GetMapping("/{businessId}/staff")
	public List<Staff> getStaffForBusiness(@PathVariable Long businessId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffService.getStaffForBusiness(businessId, principal);
	}

	@GetMapping("/{businessId}/staff/{staffId}")
	public Staff getStaffById(@PathVariable long businessId, @PathVariable long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffService.getStaffById(businessId, staffId, principal);
	}

	@PutMapping("/{businessId}/staff/{staffId}")
	public void updateStaffForBusiness(@PathVariable long businessId, @PathVariable long staffId,
			@Valid @RequestBody UpdateStaffRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		staffService.updateStaffById(businessId, staffId, request.toStaff(), principal);
	}

	@DeleteMapping("/{businessId}/staff/{staffId}")
	public void deleteStaffForBusiness(@PathVariable long businessId, @PathVariable long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		staffService.deleteStaffById(businessId, staffId, principal);
	}

	@GetMapping("/{businessId}/staff/{staffId}/services")
	public List<SalonService> getServicesOfStaff(@PathVariable long businessId, @PathVariable long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffService.getServicesOfStaff(businessId, staffId, principal);
	}

	/** Replaces the set and returns the services as stored, by name. */
	@PutMapping("/{businessId}/staff/{staffId}/services")
	public List<SalonService> replaceServicesOfStaff(@PathVariable long businessId, @PathVariable long staffId,
			@Valid @RequestBody StaffServicesRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffService.replaceServicesOfStaff(businessId, staffId, request.serviceIds(), principal);
	}
}
