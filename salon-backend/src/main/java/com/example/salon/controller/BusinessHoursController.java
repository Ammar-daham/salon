package com.example.salon.controller;

import com.example.salon.dto.BusinessHoursRequest;
import com.example.salon.model.BusinessHours;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.BusinessHoursService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * A salon's weekly opening hours, under {@code /api/v1/businesses/{businessId}/hours}.
 *
 * <p>The week is read and written whole, on the salon's clock: each interval is a weekday and two
 * wall-clock times in the salon's {@code timezone}. A day with no interval is closed; two on one day are,
 * say, either side of a lunch break. Opening hours shape the times the availability endpoint offers, but
 * booking doesn't refuse a time outside them.
 */
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

	/**
	 * Returns a salon's opening hours for the week.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/hours}. Anyone signed in who may see the salon, as for
	 * {@code GET /api/v1/businesses/{id}}. The intervals come in order, Monday first and then by opening
	 * time, along with the salon's {@code timezone}.
	 *
	 * <p>Errors: 404 if there's no such salon, or the caller may not see it.
	 *
	 * @param businessId the salon's id
	 * @param principal the signed-in caller
	 * @return 200 with the week, as {@link BusinessHours}
	 */
	@GetMapping("/{businessId}/hours")
	public BusinessHours getHours(@PathVariable long businessId, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return businessHoursService.getHours(businessId, principal);
	}

	/**
	 * Replaces a salon's whole week of opening hours.
	 *
	 * <p>{@code PUT /api/v1/businesses/{businessId}/hours}. An ADMIN of the salon, or a SUPER_ADMIN. A day
	 * that isn't listed is closed, and an empty list closes the salon every day. Each interval must close
	 * after it opens, so none runs past midnight, and a day's intervals may touch but not overlap. Nothing
	 * is stored unless the whole week passes.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link BusinessHoursRequest}, an interval doesn't close
	 * after it opens, or two on one day overlap; 403 for another salon; 404 if there's no such salon.
	 *
	 * @param businessId the salon's id
	 * @param request the whole week, as a {@link BusinessHoursRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the week as stored, in order, with the salon's {@code timezone}
	 */
	@PutMapping("/{businessId}/hours")
	public BusinessHours replaceHours(@PathVariable long businessId, @Valid @RequestBody BusinessHoursRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return businessHoursService.replaceHours(businessId, request.toOpeningIntervals(), principal);
	}
}
