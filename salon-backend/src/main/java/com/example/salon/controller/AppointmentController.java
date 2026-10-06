package com.example.salon.controller;

import com.example.salon.dto.AppointmentRequest;
import com.example.salon.dto.AppointmentStatusRequest;
import com.example.salon.model.Appointment;
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
import java.util.List;

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

	/** By start time. from and to are "yyyy-MM-dd" on the salon's clock, both included; every filter is optional. */
	@GetMapping
	public List<Appointment> getAppointments(@PathVariable long businessId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(name = "staff_id", required = false) Long staffId,
			@RequestParam(name = "customer_id", required = false) Long customerId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return appointmentService.getAppointments(businessId, from, to, staffId, customerId, principal);
	}

	@GetMapping("/{appointmentId}")
	public Appointment getAppointmentById(@PathVariable long businessId, @PathVariable long appointmentId,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return appointmentService.getAppointmentById(businessId, appointmentId, principal);
	}

	/** Returns the appointment as stored, since the server works out ends_at and price. */
	@PutMapping("/{appointmentId}")
	public Appointment updateAppointment(@PathVariable long businessId, @PathVariable long appointmentId,
			@Valid @RequestBody AppointmentRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return appointmentService.updateAppointment(businessId, appointmentId, request.toBooking(), principal);
	}

	/** Confirms, completes, cancels or marks a no-show, and returns the appointment. There is no DELETE: cancel it. */
	@PutMapping("/{appointmentId}/status")
	public Appointment changeStatus(@PathVariable long businessId, @PathVariable long appointmentId,
			@Valid @RequestBody AppointmentStatusRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return appointmentService.changeStatus(businessId, appointmentId, request.status(), principal);
	}
}
