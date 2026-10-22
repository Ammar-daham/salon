package com.example.salon.controller;

import com.example.salon.dto.RegistrationRequest;
import com.example.salon.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registering a salon, under {@code /api/v1/auth}: how a salon owner joins the platform without a
 * SUPER_ADMIN creating the salon for them. A SUPER_ADMIN still can, with {@code POST /api/v1/businesses}.
 */
@RequestMapping("api/v1/auth")
@RestController
public class RegistrationController
{
	private final RegistrationService registrationService;

	@Autowired
	public RegistrationController(RegistrationService registrationService)
	{
		this.registrationService = registrationService;
	}

	/**
	 * Registers a salon and its owner's account, for a SUPER_ADMIN to review.
	 *
	 * <p>{@code POST /api/v1/auth/register}. Needs no session. The salon starts {@code PENDING}, whatever
	 * {@code status} the body gives it, and the owner is its ADMIN. Every SUPER_ADMIN gets a
	 * {@code BUSINESS_REGISTERED} notification. The owner can sign in once a SUPER_ADMIN approves the salon,
	 * and gets a 403 saying it's waiting until then. Answers 202 with no body.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link RegistrationRequest}, each field named by its
	 * path, such as {@code salon.name} or {@code owner.email}; 409 if the salon's name is taken, the email
	 * already has an account, or the salon lists a contact twice, and then nothing is created; 429, with
	 * {@code Retry-After} in seconds, after too many registrations from one address, by default 5 an hour.
	 *
	 * @param request the owner and the salon, a {@link RegistrationRequest}
	 * @param servletRequest the HTTP request, for the client's address
	 */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void register(@Valid @RequestBody RegistrationRequest request, HttpServletRequest servletRequest)
	{
		registrationService.register(request.salon().toBusiness(), request.owner().toUser(),
				servletRequest.getRemoteAddr());
	}
}
