package com.example.salon.controller;

import com.example.salon.dto.SalonServiceRequest;
import com.example.salon.model.SalonService;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.BusinessSalonServiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * A salon's services and prices, under {@code /api/v1/businesses/{businessId}/services}.
 *
 * <p>Anyone who may see the salon may read a service; an ADMIN of the salon or a SUPER_ADMIN adds,
 * changes and removes them. There is no list here: a salon's services come in {@code services} on
 * {@code GET /api/v1/businesses/{id}}.
 */
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

	/**
	 * Adds a service to a salon's menu.
	 *
	 * <p>{@code POST /api/v1/businesses/{businessId}/services}. An ADMIN of the salon, or a SUPER_ADMIN.
	 * {@code price} is in the salon's {@code currency}, and only an active service can be booked.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link SalonServiceRequest}; 403 for another salon; 404 if
	 * the salon doesn't exist.
	 *
	 * @param businessId the salon's id
	 * @param request the service, as a {@link SalonServiceRequest}
	 * @param principal the signed-in caller
	 * @return 201 Created with the service as stored, and its address in {@code Location}
	 */
	@PostMapping("/{businessId}/services")
	public ResponseEntity<SalonService> createServiceForBusiness(
			@PathVariable Long businessId,
			@Valid @RequestBody SalonServiceRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal
	)
	{
		SalonService salonService = request.toSalonService();
		SalonService s = businessSalonServiceService
				.createServiceForBusiness(businessId, salonService, principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(s.getId())
				.toUri();

		return ResponseEntity.created(location).body(s);
	}

	/**
	 * Returns one of a salon's services.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/services/{serviceId}}. Anyone signed in who may see the
	 * salon, as for {@code GET /api/v1/businesses/{id}}. Inactive services are returned too.
	 *
	 * <p>Errors: 404 if there's no such salon or the caller may not see it, or the salon has no such
	 * service, or it has been removed.
	 *
	 * @param businessId the salon's id
	 * @param serviceId the service's id
	 * @param principal the signed-in caller
	 * @return 200 with the service
	 */
	@GetMapping("/{businessId}/services/{serviceId}")
	public SalonService getServiceForBusiness(@PathVariable long businessId, @PathVariable long serviceId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return businessSalonServiceService.getServiceById(businessId, serviceId, principal);
	}

	/**
	 * Replaces a service's details.
	 *
	 * <p>{@code PUT /api/v1/businesses/{businessId}/services/{serviceId}}. An ADMIN of the salon, or a
	 * SUPER_ADMIN. Every field is replaced, so leaving {@code description} out clears it. Appointments
	 * already booked keep the length and price they were booked with.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link SalonServiceRequest}; 403 for another salon; 404
	 * if the salon has no such service, or it has been removed.
	 *
	 * @param businessId the salon's id
	 * @param serviceId the service's id
	 * @param request the new details, as a {@link SalonServiceRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the service as stored
	 */
	@PutMapping("/{businessId}/services/{serviceId}")
	public SalonService updateServiceForBusiness(@PathVariable long businessId, @PathVariable long serviceId,
			@Valid @RequestBody SalonServiceRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return businessSalonServiceService.updateServiceForBusiness(businessId, serviceId, request.toSalonService(), principal);
	}

	/**
	 * Removes a service from a salon's menu.
	 *
	 * <p>{@code DELETE /api/v1/businesses/{businessId}/services/{serviceId}}. An ADMIN of the salon, or a
	 * SUPER_ADMIN. A soft delete: past appointments keep pointing at the service, but it's gone from the
	 * salon and from the services its staff perform, and it can't be read, changed or booked any more.
	 * Answers 204.
	 *
	 * <p>Errors: 403 for another salon; 404 if the salon has no such service, or it was already removed; 409
	 * while the service has upcoming appointments ({@code BOOKED} or {@code CONFIRMED}, and not over yet).
	 *
	 * @param businessId the salon's id
	 * @param serviceId the service's id
	 * @param principal the signed-in caller
	 */
	@DeleteMapping("/{businessId}/services/{serviceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteServiceForBusiness(@PathVariable long businessId, @PathVariable long serviceId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		businessSalonServiceService.deleteServiceForBusiness(businessId, serviceId, principal);
	}
}
