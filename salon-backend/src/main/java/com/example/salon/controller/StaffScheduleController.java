package com.example.salon.controller;

import com.example.salon.dto.StaffScheduleRequest;
import com.example.salon.dto.TimeOffRequest;
import com.example.salon.model.StaffSchedule;
import com.example.salon.model.TimeOff;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.StaffScheduleService;
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
 * A staff member's working week and time off, under {@code /api/v1/businesses/{businessId}/staff/{staffId}}.
 *
 * <p>Both are on the salon's clock, in its {@code timezone}, and only an ADMIN of the salon or a
 * SUPER_ADMIN changes them. The salon's staff may read anyone's week; time off can say why someone is
 * away, so only an ADMIN of the salon, a SUPER_ADMIN and the staff member themselves read it. A removed
 * staff member's week and time off answer 404.
 */
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

	/**
	 * Returns a staff member's working week.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/staff/{staffId}/schedule}. A SUPER_ADMIN, or an ADMIN
	 * or EMPLOYEE of the salon. The shifts come in order, Monday first and then by start, along with the
	 * salon's {@code timezone}.
	 *
	 * <p>Errors: 403 for another salon; 404 if there's no such salon, or it has no such staff member.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param principal the signed-in caller
	 * @return 200 with the week, as {@link StaffSchedule}
	 */
	@GetMapping("/schedule")
	public StaffSchedule getSchedule(@PathVariable long businessId, @PathVariable long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffScheduleService.getSchedule(businessId, staffId, principal);
	}

	/**
	 * Replaces a staff member's whole working week.
	 *
	 * <p>{@code PUT /api/v1/businesses/{businessId}/staff/{staffId}/schedule}. An ADMIN of the salon, or a
	 * SUPER_ADMIN. A day that isn't listed is a day off, and an empty list leaves no working days. Each
	 * shift must end after it starts, so none runs past midnight, and a day's shifts may touch but not
	 * overlap. A shift outside the salon's opening hours is accepted; the availability endpoint only offers
	 * times inside both. Nothing is stored unless the whole week passes.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link StaffScheduleRequest}, a shift doesn't end after
	 * it starts, or two on one day overlap; 403 for another salon; 404 if there's no such salon, or it has no
	 * such staff member.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param request the whole week, as a {@link StaffScheduleRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the week as stored, in order, with the salon's {@code timezone}
	 */
	@PutMapping("/schedule")
	public StaffSchedule replaceSchedule(@PathVariable long businessId, @PathVariable long staffId,
			@Valid @RequestBody StaffScheduleRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffScheduleService.replaceSchedule(businessId, staffId, request.toWorkingIntervals(), principal);
	}

	/**
	 * Records an absence for a staff member.
	 *
	 * <p>{@code POST /api/v1/businesses/{businessId}/staff/{staffId}/time-off}. An ADMIN of the salon, or a
	 * SUPER_ADMIN. {@code starts_at} and {@code ends_at} are on the salon's clock; a whole day off runs from
	 * 00:00 to 00:00 the next day. One person's absences may touch but not overlap. The availability
	 * endpoint offers no times during an absence, but booking doesn't refuse one.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link TimeOffRequest}, or {@code ends_at} isn't after
	 * {@code starts_at}; 403 for another salon; 404 if there's no such salon, or it has no such staff member;
	 * 409 if it overlaps another absence of the same person.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param request the absence, as a {@link TimeOffRequest}
	 * @param principal the signed-in caller
	 * @return 201 Created with the time off as stored, and its address in {@code Location}
	 */
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

	/**
	 * Lists a staff member's time off.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/staff/{staffId}/time-off}. An ADMIN of the salon, a
	 * SUPER_ADMIN, or the staff member themselves. Every absence, past ones too, in order of start, on the
	 * salon's clock.
	 *
	 * <p>Errors: 403 for another salon, or for a colleague who isn't the staff member; 404 if there's no such
	 * salon, or it has no such staff member.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param principal the signed-in caller
	 * @return 200 with the absences
	 */
	@GetMapping("/time-off")
	public List<TimeOff> getTimeOff(@PathVariable long businessId, @PathVariable long staffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffScheduleService.getTimeOff(businessId, staffId, principal);
	}

	/**
	 * Returns one of a staff member's absences.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/staff/{staffId}/time-off/{timeOffId}}. An ADMIN of the
	 * salon, a SUPER_ADMIN, or the staff member themselves.
	 *
	 * <p>Errors: 403 for another salon, or for a colleague who isn't the staff member; 404 if there's no such
	 * salon or staff member, or the staff member has no such time off.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param timeOffId the absence's id
	 * @param principal the signed-in caller
	 * @return 200 with the time off
	 */
	@GetMapping("/time-off/{timeOffId}")
	public TimeOff getTimeOffById(@PathVariable long businessId, @PathVariable long staffId,
			@PathVariable long timeOffId, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffScheduleService.getTimeOffById(businessId, staffId, timeOffId, principal);
	}

	/**
	 * Replaces an absence's times and note.
	 *
	 * <p>{@code PUT /api/v1/businesses/{businessId}/staff/{staffId}/time-off/{timeOffId}}. An ADMIN of the
	 * salon, or a SUPER_ADMIN. Every field is replaced, so leaving {@code note} out clears it.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link TimeOffRequest}, or {@code ends_at} isn't after
	 * {@code starts_at}; 403 for another salon; 404 if there's no such salon or staff member, or the staff
	 * member has no such time off; 409 if it would overlap another absence of the same person.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param timeOffId the absence's id
	 * @param request the new times and note, as a {@link TimeOffRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the time off as stored
	 */
	@PutMapping("/time-off/{timeOffId}")
	public TimeOff updateTimeOff(@PathVariable long businessId, @PathVariable long staffId, @PathVariable long timeOffId,
			@Valid @RequestBody TimeOffRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return staffScheduleService.updateTimeOff(businessId, staffId, timeOffId, request.toTimeOff(), principal);
	}

	/**
	 * Deletes an absence.
	 *
	 * <p>{@code DELETE /api/v1/businesses/{businessId}/staff/{staffId}/time-off/{timeOffId}}. An ADMIN of
	 * the salon, or a SUPER_ADMIN. A hard delete. Answers 204.
	 *
	 * <p>Errors: 403 for another salon; 404 if the salon has no such staff member, or the staff member has no
	 * such time off.
	 *
	 * @param businessId the salon's id
	 * @param staffId the staff record's id
	 * @param timeOffId the absence's id
	 * @param principal the signed-in caller
	 */
	@DeleteMapping("/time-off/{timeOffId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTimeOff(@PathVariable long businessId, @PathVariable long staffId, @PathVariable long timeOffId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		staffScheduleService.deleteTimeOff(businessId, staffId, timeOffId, principal);
	}
}
