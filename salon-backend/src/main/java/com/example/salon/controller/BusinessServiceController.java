package com.example.salon.controller;

import com.example.salon.model.SalonService;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.BusinessSalonServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("api/v1/businesses")
public class BusinessServiceController
{

	private final BusinessSalonServiceService businessSalonServiceService;

	@Autowired
	public BusinessServiceController(BusinessSalonServiceService businessSalonServiceService)
	{
		this.businessSalonServiceService = businessSalonServiceService;
	}

	@PostMapping("/{businessId}/services")
	public SalonService createServiceForBusiness(
			@PathVariable Long businessId,
			@RequestBody SalonService salonService,
			@AuthenticationPrincipal AuthenticatedUser principal
	)
	{
		SalonService s = businessSalonServiceService
				.createServiceForBusiness(businessId, salonService, principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(salonService.getId())
				.toUri();

		return ResponseEntity.created(location).body(s).getBody();
	}

	@GetMapping("/{businessId}/services/{serviceId}")
	public SalonService getServiceForBusiness(@PathVariable int businessId, @PathVariable int serviceId)
	{
		return businessSalonServiceService.getServiceById(businessId, serviceId);
	}

	@PutMapping("/{businessId}/services/{serviceId}")
	public void updateServiceForBusiness(@PathVariable int businessId, @PathVariable int serviceId,
			@RequestBody SalonService salonService, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		businessSalonServiceService.updateServiceForBusiness(businessId, serviceId, salonService, principal);
	}

	@DeleteMapping("/{businessId}/services/{serviceId}")
	public void deleteServiceForBusiness(@PathVariable int businessId, @PathVariable int serviceId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		businessSalonServiceService.deleteServiceForBusiness(businessId, serviceId, principal);
	}
}
