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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * A salon's staff, and the services each performs, under {@code /api/v1/businesses/{businessId}/staff}.
 *
 * <p>A staff record says that one of the salon's users works there. Anyone who may see the salon may read
 * the staff; only an ADMIN of the salon or a SUPER_ADMIN sees their emails or changes anything.
 */
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

	/**
	 * Takes one of the salon's users on as staff.
	 *
	 * <p>{@code POST /api/v1/businesses/{businessId}/staff}. An ADMIN of the salon, or a SUPER_ADMIN.
	 * {@code user_id} must be an EMPLOYEE or ADMIN whose {@code business_id} is this salon, with no current
	 * staff record; someone who was removed from the staff can be taken on again, as a new record. The
	 * answer carries the person's {@code first_name}, {@code last_name} and {@code email}.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link CreateStaffRequest}, or the user doesn't belong
	 * to the salon or isn't an EMPLOYEE or ADMIN; 403 for another salon; 404 if there's no such user; 409 if
	 * the user already has a current staff record.
	 *
	 * @param businessId the salon's id
	 * @param request the staff record, as a {@link CreateStaffRequest}
	 * @param principal the signed-in caller
	 * @return 201 Created with the staff record, and its address in {@code Location}
	 */
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

	/**
	 * Lists a salon's staff.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/staff}. Anyone signed in who may see the salon, as for
	 * {@code GET /api/v1/businesses/{id}}. Every current staff record, active or not, in the order they were
	 * added, with each person's name. {@code email} is filled in only for an ADMIN of the salon or a
	 * SUPER_ADMIN, and null for anyone else.
	 *
	 * <p>Errors: 404 if there's no such salon or the caller may not see it.
	 *
	 * @param businessId the salon's id
	 * @param principal the signed-in caller
	 * @return 200 with the staff records
	 */
	@GetMapping("/{businessId}/staff")
	public List<Staff> getStaffForBusiness(@PathVariable Long businessId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffService.getStaffForBusiness(businessId, principal);
	}

	/**
	 * Returns one of a salon's staff records.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/staff/{staffId}}. Anyone signed in who may see the
	 * salon, as for {@code GET /api/v1/businesses/{id}}. {@code email} is filled in only for an ADMIN of the
	 * salon or a SUPER_ADMIN.
	 *
	 * <p>Errors: 404 if there's no such salon or the caller may not see it, or the salon has no such staff
	 * record, or it has been removed.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param principal the signed-in caller
	 * @return 200 with the staff record
	 */
	@GetMapping("/{businessId}/staff/{staffId}")
	public Staff getStaffById(@PathVariable long businessId, @PathVariable long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffService.getStaffById(businessId, staffId, principal);
	}

	/**
	 * Replaces a staff record's details.
	 *
	 * <p>{@code PUT /api/v1/businesses/{businessId}/staff/{staffId}}. An ADMIN of the salon, or a
	 * SUPER_ADMIN. {@code title}, {@code is_active}, {@code hired_at} and {@code calendar_colour} are all
	 * replaced, so leaving {@code calendar_colour} out clears it; who the record is for can't change. An
	 * inactive staff member can't be booked.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link UpdateStaffRequest}; 403 for another salon; 404 if
	 * the salon has no such staff record, or it has been removed.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param request the new details, as an {@link UpdateStaffRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the staff record as stored
	 */
	@PutMapping("/{businessId}/staff/{staffId}")
	public Staff updateStaffForBusiness(@PathVariable long businessId, @PathVariable long staffId,
			@Valid @RequestBody UpdateStaffRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffService.updateStaffById(businessId, staffId, request.toStaff(), principal);
	}

	/**
	 * Removes someone from a salon's staff.
	 *
	 * <p>{@code DELETE /api/v1/businesses/{businessId}/staff/{staffId}}. An ADMIN of the salon, or a
	 * SUPER_ADMIN. A soft delete: past appointments keep pointing at the record, but it, and its services,
	 * week and time off, can't be read or changed any more. The user account stays, and the person can be
	 * taken on again as a new staff record. Answers 204.
	 *
	 * <p>Errors: 403 for another salon; 404 if the salon has no such staff record, or it was already removed;
	 * 409 while the staff member has upcoming appointments ({@code BOOKED} or {@code CONFIRMED}, and not over
	 * yet).
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param principal the signed-in caller
	 */
	@DeleteMapping("/{businessId}/staff/{staffId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteStaffForBusiness(@PathVariable long businessId, @PathVariable long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		staffService.deleteStaffById(businessId, staffId, principal);
	}

	/**
	 * Lists the services a staff member performs.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/staff/{staffId}/services}. Anyone signed in who may see
	 * the salon, as for {@code GET /api/v1/businesses/{id}}. Sorted by name; removed services are left out.
	 *
	 * <p>Errors: 404 if there's no such salon or the caller may not see it, or the salon has no such staff
	 * record.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param principal the signed-in caller
	 * @return 200 with the services
	 */
	@GetMapping("/{businessId}/staff/{staffId}/services")
	public List<SalonService> getServicesOfStaff(@PathVariable long businessId, @PathVariable long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffService.getServicesOfStaff(businessId, staffId, principal);
	}

	/**
	 * Replaces the services a staff member performs.
	 *
	 * <p>{@code PUT /api/v1/businesses/{businessId}/staff/{staffId}/services}. An ADMIN of the salon, or a
	 * SUPER_ADMIN. The body lists every service the person performs, and an empty list leaves none. Each
	 * must be one of the salon's current services, active or not; a repeated id counts once. A staff
	 * member can only be booked for a service they perform.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link StaffServicesRequest}, or an id isn't one of the
	 * salon's services; 403 for another salon; 404 if the salon has no such staff record.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param request the services, as a {@link StaffServicesRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the services the staff member now performs, sorted by name
	 */
	@PutMapping("/{businessId}/staff/{staffId}/services")
	public List<SalonService> replaceServicesOfStaff(@PathVariable long businessId, @PathVariable long staffId,
			@Valid @RequestBody StaffServicesRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffService.replaceServicesOfStaff(businessId, staffId, request.serviceIds(), principal);
	}
}
