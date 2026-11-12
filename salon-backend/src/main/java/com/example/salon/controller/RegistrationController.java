package com.example.salon.controller;

import com.example.salon.dto.RegistrationRequest;
import com.example.salon.dto.SignUpRequest;
import com.example.salon.model.User;
import com.example.salon.service.RegistrationService;
import com.example.salon.service.SignUpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Creating an account without a session, under {@code /api/v1/auth}: a salon owner registering their salon,
 * so a SUPER_ADMIN needn't create it for them (a SUPER_ADMIN still can, with {@code POST /api/v1/businesses}),
 * and a client signing up to book in the customer app.
 */
@RequestMapping("api/v1/auth")
@RestController
public class RegistrationController
{
	private final RegistrationService registrationService;
	private final SignUpService signUpService;

	@Autowired
	public RegistrationController(RegistrationService registrationService, SignUpService signUpService)
	{
		this.registrationService = registrationService;
		this.signUpService = signUpService;
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

	/**
	 * Creates a client's own account, to book with in the customer app.
	 *
	 * <p>{@code POST /api/v1/auth/sign-up}. Needs no session. The account is a CUSTOMER's with no salon,
	 * whatever the body says; it meets a salon's client record when the client first books there. It doesn't
	 * sign in: the client signs in next with the same email and password. Answers 201 with the account as
	 * stored, like {@code GET /api/v1/users/{id}}, and that address in {@code Location}.
	 *
	 * <p>Errors: 400 if the body breaks the rules of {@link SignUpRequest}, each field named, such as
	 * {@code email}; 409 if the email already has an account, in any case, and then nothing is created; 429,
	 * with {@code Retry-After} in seconds, after too many sign-ups from one address, by default 5 an hour,
	 * counted apart from salon registrations.
	 *
	 * @param request the client's name, email and password, a {@link SignUpRequest}
	 * @param servletRequest the HTTP request, for the client's address
	 * @return 201 with the account
	 */
	@PostMapping("/sign-up")
	public ResponseEntity<User> signUp(@Valid @RequestBody SignUpRequest request, HttpServletRequest servletRequest)
	{
		User account = signUpService.signUp(request.toUser(), servletRequest.getRemoteAddr());
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
				.path("/api/v1/users/{id}")
				.buildAndExpand(account.getId())
				.toUri();
		return ResponseEntity.created(location).body(account);
	}
}
