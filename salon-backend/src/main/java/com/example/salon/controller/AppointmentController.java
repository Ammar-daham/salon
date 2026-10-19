package com.example.salon.controller;

import com.example.salon.dto.AppointmentRequest;
import com.example.salon.dao.AppointmentDao;
import com.example.salon.dto.AppointmentStatusRequest;
import com.example.salon.model.Appointment;
import com.example.salon.model.AppointmentStatus;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

/**
 * Appointments at one salon, under {@code /api/v1/businesses/{businessId}/appointments}.
 *
 * <p>The salon's ADMIN and EMPLOYEE users and a SUPER_ADMIN may call every endpoint here, as a front desk does.
 * Times are {@code yyyy-MM-dd'T'HH:mm} on the salon's clock, in its {@code timezone}. An appointment is never
 * deleted: it is cancelled, and stays in the history.
 */
@RestController
@RequestMapping("api/v1/businesses/{businessId}/appointments")
public class AppointmentController
{
	private final AppointmentService appointmentService;

	@Autowired
	public AppointmentController(AppointmentService appointmentService)
	{
		this.appointmentService = appointmentService;
	}

	/**
	 * Books an appointment: one service, by one staff member, for one of the salon's customers.
	 *
	 * <p>{@code POST /api/v1/businesses/{businessId}/appointments}. ADMIN and EMPLOYEE of the salon, or a
	 * SUPER_ADMIN. The customer must be a current customer of the salon, the staff member an active member of its
	 * staff, and the service one of its active services that the staff member performs. The service sets
	 * {@code ends_at} and {@code price}, and the appointment starts out {@code BOOKED}. Opening hours, shifts,
	 * time off and whether the time has passed aren't checked (availability only advises), but a staff member
	 * can't be booked twice at once. Back to back is fine, and a cancelled or missed appointment frees its time.
	 *
	 * <p>Errors: 400 if the body fails validation, or the customer, staff member or service doesn't qualify; 403
	 * for another salon; 404 if the salon doesn't exist; 409 if the staff member already has an appointment then.
	 *
	 * @param businessId the salon
	 * @param request the customer, staff member, service, start and notes: see {@link AppointmentRequest}
	 * @param principal the signed-in caller
	 * @return 201 Created with the appointment, and its address in {@code Location}
	 */
	@PostMapping
	public ResponseEntity<Appointment> book(@PathVariable long businessId,
			@Valid @RequestBody AppointmentRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		Appointment appointment = appointmentService.book(businessId, request.toBooking(), principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(appointment.id())
				.toUri();

		return ResponseEntity.created(location).body(appointment);
	}

	/**
	 * Lists a salon's appointments, a page at a time.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/appointments}. ADMIN and EMPLOYEE of the salon, or a
	 * SUPER_ADMIN. Every filter is optional: {@code from} and {@code to} keep the appointments that start on those
	 * days of the salon's clock, both included; {@code staff_id} and {@code customer_id} keep one staff member's
	 * or one customer's; and {@code status} keeps those with that status, where leaving it out includes every
	 * status. {@code sort} is {@code starts_at} (the default, soonest first) or {@code -starts_at} (latest first).
	 *
	 * <p>Errors: 400 for a malformed filter, {@code to} before {@code from}, or a bad {@code page}, {@code size}
	 * or {@code sort}; 403 for another salon; 404 if the salon doesn't exist.
	 *
	 * @param businessId the salon
	 * @param from the first day, {@code yyyy-MM-dd}, or nothing for no first day
	 * @param to the last day, {@code yyyy-MM-dd}, or nothing for no last day
	 * @param staffId only this staff member's appointments, or nothing for everyone's
	 * @param customerId only this customer's appointments, or nothing for everyone's
	 * @param status only appointments with this status: {@code BOOKED}, {@code CONFIRMED}, {@code COMPLETED},
	 *        {@code CANCELLED} or {@code NO_SHOW}
	 * @param sort the order: {@code starts_at}, or {@code -starts_at} for latest first
	 * @param page the page, counting from 1 (default 1)
	 * @param size appointments per page, 1 to 100 (default 20)
	 * @param principal the signed-in caller
	 * @return 200 with a page of appointments
	 */
	@GetMapping
	public Page<Appointment> getAppointments(@PathVariable long businessId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(name = "staff_id", required = false) Long staffId,
			@RequestParam(name = "customer_id", required = false) Long customerId,
			@RequestParam(required = false) AppointmentStatus status,
			@RequestParam(defaultValue = "starts_at") String sort,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return appointmentService.getAppointments(businessId, from, to, staffId, customerId, status,
				Sort.parse(sort, AppointmentDao.SortBy.class), PageQuery.of(page, size), principal);
	}

	/**
	 * Returns one of a salon's appointments.
	 *
	 * <p>{@code GET /api/v1/businesses/{businessId}/appointments/{appointmentId}}. ADMIN and EMPLOYEE of the salon,
	 * or a SUPER_ADMIN. It still names its customer, staff member and service once they have been removed.
	 *
	 * <p>Errors: 403 for another salon; 404 if the salon doesn't exist or the appointment isn't one of its own.
	 *
	 * @param businessId the salon
	 * @param appointmentId the appointment
	 * @param principal the signed-in caller
	 * @return 200 with the appointment
	 */
	@GetMapping("/{appointmentId}")
	public Appointment getAppointmentById(@PathVariable long businessId, @PathVariable long appointmentId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return appointmentService.getAppointmentById(businessId, appointmentId, principal);
	}

	/**
	 * Changes an appointment's customer, staff member, service, start or notes.
	 *
	 * <p>{@code PUT /api/v1/businesses/{businessId}/appointments/{appointmentId}}. ADMIN and EMPLOYEE of the salon,
	 * or a SUPER_ADMIN. Only a {@code BOOKED} or {@code CONFIRMED} appointment can be changed, and its status stays
	 * as it is. Every field is replaced. Only what changes is checked against the booking rules, so an appointment
	 * whose staff member or service has since been deactivated can still be moved. It keeps its length and price
	 * unless the service changes, when it takes the new service's.
	 *
	 * <p>Errors: 400 if the body fails validation, or a new customer, staff member or service doesn't qualify; 403
	 * for another salon; 404 if the salon doesn't exist or the appointment isn't one of its own; 409 if the
	 * appointment is {@code COMPLETED}, {@code CANCELLED} or a {@code NO_SHOW}, or the staff member already has an
	 * appointment then.
	 *
	 * @param businessId the salon
	 * @param appointmentId the appointment
	 * @param request the customer, staff member, service, start and notes: see {@link AppointmentRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the appointment as stored, since the server works out {@code ends_at} and {@code price}
	 */
	@PutMapping("/{appointmentId}")
	public Appointment updateAppointment(@PathVariable long businessId, @PathVariable long appointmentId,
			@Valid @RequestBody AppointmentRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return appointmentService.updateAppointment(businessId, appointmentId, request.toBooking(), principal);
	}

	/**
	 * Changes an appointment's status: confirms, completes or cancels it, or marks it a no-show.
	 *
	 * <p>{@code PUT /api/v1/businesses/{businessId}/appointments/{appointmentId}/status}. ADMIN and EMPLOYEE of the
	 * salon, or a SUPER_ADMIN. It moves {@code BOOKED} &rarr; {@code CONFIRMED} &rarr; {@code COMPLETED},
	 * {@code CANCELLED} or {@code NO_SHOW}: confirming is optional, nothing goes back, and the last three are
	 * final (see {@link com.example.salon.model.AppointmentStatus}). It can only be {@code COMPLETED} or a
	 * {@code NO_SHOW} once it has started. Asking for the status it already has changes nothing. There is no
	 * DELETE: cancel it instead, which frees the staff member's time.
	 *
	 * <p>Errors: 400 if {@code status} is missing or not one of the five; 403 for another salon; 404 if the salon
	 * doesn't exist or the appointment isn't one of its own; 409 if the move isn't allowed, or is to
	 * {@code COMPLETED} or {@code NO_SHOW} before the start.
	 *
	 * @param businessId the salon
	 * @param appointmentId the appointment
	 * @param request the new {@code status}: see {@link AppointmentStatusRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the appointment
	 */
	@PutMapping("/{appointmentId}/status")
	public Appointment changeStatus(@PathVariable long businessId, @PathVariable long appointmentId,
			@Valid @RequestBody AppointmentStatusRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return appointmentService.changeStatus(businessId, appointmentId, request.status(), principal);
	}
}
