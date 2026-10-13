package com.example.salon.controller;

import com.example.salon.dto.ChangePasswordRequest;
import com.example.salon.dto.ForgotPasswordRequest;
import com.example.salon.dto.ResetPasswordRequest;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.logging.RequestLog;
import com.example.salon.model.User;
import com.example.salon.security.AuthUserResponse;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.security.LoginRequest;
import com.example.salon.security.LoginThrottle;
import com.example.salon.service.PasswordResetService;
import com.example.salon.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signing in and out, and passwords, under {@code /api/v1/auth}.
 *
 * <p>{@code /login}, {@code /forgot-password} and {@code /reset-password} need no session; the rest are
 * for any signed-in user, about themselves. Failed sign-ins are counted per email and per client address,
 * and a wrong current password on {@code /change-password} counts as one.
 */
@RequestMapping("api/v1/auth")
@RestController
public class AuthController
{
	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final LoginThrottle loginThrottle;
	private final PasswordEncoder passwordEncoder;
	private final UserService userService;
	private final PasswordResetService passwordResetService;

	@Autowired
	public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
			LoginThrottle loginThrottle, PasswordEncoder passwordEncoder, UserService userService,
			PasswordResetService passwordResetService)
	{
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.loginThrottle = loginThrottle;
		this.passwordEncoder = passwordEncoder;
		this.userService = userService;
		this.passwordResetService = passwordResetService;
	}

	/**
	 * Signs a user in with their email and password, and starts their session.
	 *
	 * <p>{@code POST /api/v1/auth/login}. Needs no session. The email's case doesn't matter, and an
	 * account with no password set can't sign in. The session is kept in the {@code JSESSIONID} cookie;
	 * a session the client already had gets a new id.
	 *
	 * <p>Errors: 401 if the email and password don't match an account that can sign in, the same answer
	 * whether or not the email has one; 429, with {@code Retry-After} in seconds, after too many failed
	 * sign-ins, by default 5 for one email or 20 from one address within 15 minutes. While locked, even
	 * the right password gets 429.
	 *
	 * @param request the email and password, a {@link LoginRequest}
	 * @param servletRequest the HTTP request
	 * @param servletResponse the HTTP response
	 * @return 200 with the signed-in user
	 */
	@PostMapping("/login")
	public AuthUserResponse login(@RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse)
	{
		// Refused before the password is checked, so while locked a right guess gets the same answer as a
		// wrong one. Behind a reverse proxy the address is the proxy's unless server.forward-headers-strategy is set.
		LoginThrottle.Attempt attempt = loginThrottle.admit(request.email(), servletRequest.getRemoteAddr());

		Authentication authResult;
		try {
			authResult = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())
			);
		} catch (AuthenticationException ex) {
			// Masked: enough to notice repeated attempts on one account, without logging the address.
			log.warn("Failed sign-in for {}", RequestLog.maskEmail(request.email()));
			attempt.failed();
			throw new BaseException("Invalid email or password", ErrorCode.UNAUTHORIZED);
		}
		attempt.succeeded();

		saveSignedIn(authResult, servletRequest, servletResponse);

		User user = ((AuthenticatedUser) authResult.getPrincipal()).getUser();
		log.info("User {} signed in", user.getId());
		return AuthUserResponse.from(user);
	}

	/**
	 * Changes the signed-in user's own password.
	 *
	 * <p>{@code POST /api/v1/auth/change-password}. Any signed-in user, for themselves.
	 * {@code current_password} must be their password now, and {@code new_password} 8 to 72 characters.
	 * A wrong current password counts as a failed sign-in for their email and the client's address. Every
	 * other session of theirs is signed out; this one stays signed in, under a new session id. Answers 204.
	 *
	 * <p>Errors: 400 if {@code current_password} is wrong (a 400, not a 401: the session stays signed in)
	 * or {@code new_password} breaks the rule; 429, with {@code Retry-After}, after too many failed
	 * sign-ins, as for {@code /login}.
	 *
	 * @param request the current and the new password, a {@link ChangePasswordRequest}
	 * @param principal the signed-in caller
	 * @param servletRequest the HTTP request
	 * @param servletResponse the HTTP response
	 */
	@PostMapping("/change-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal,
			HttpServletRequest servletRequest, HttpServletResponse servletResponse)
	{
		User user = principal.getUser();
		// A wrong current password counts as a failed sign-in, so a stolen session can't be used to guess it.
		LoginThrottle.Attempt attempt = loginThrottle.admit(user.getEmail(), servletRequest.getRemoteAddr());
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			attempt.failed();
			throw new BaseException("Current password is incorrect", ErrorCode.BAD_REQUEST);
		}
		attempt.succeeded();

		User updated = userService.setPassword(user.getId(), request.newPassword());
		AuthenticatedUser signedIn = new AuthenticatedUser(updated);
		saveSignedIn(UsernamePasswordAuthenticationToken.authenticated(signedIn, null, signedIn.getAuthorities()),
				servletRequest, servletResponse);
		log.info("User {} changed their password", user.getId());
	}

	/**
	 * Emails a password reset link, if the email has an account that can sign in.
	 *
	 * <p>{@code POST /api/v1/auth/forgot-password}. Needs no session. The email's case doesn't matter. The
	 * answer is the same whether or not the email has an account, and comes before it is looked up; the
	 * link is sent in the background. It opens the admin panel's reset page with a {@code token}
	 * parameter, works once, and expires after an hour by default. Asking again retires the earlier links,
	 * and a user is sent at most 3 links an hour by default; past that nothing is sent, with the same
	 * answer. Answers 202 with no body.
	 *
	 * <p>Errors: 400 if {@code email} is missing or isn't an email address.
	 *
	 * @param request the email to send the link to, a {@link ForgotPasswordRequest}
	 */
	@PostMapping("/forgot-password")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request)
	{
		passwordResetService.requestReset(request.email());
	}

	/**
	 * Sets a new password with the token from a reset link.
	 *
	 * <p>{@code POST /api/v1/auth/reset-password}. Needs no session. {@code token} is the link's
	 * {@code token} parameter, and {@code password} the new password, 8 to 72 characters. The link is used
	 * up, along with any other the user still has. Every session of the user is signed out, and the failed
	 * sign-ins counted against their email are forgotten. Answers 204.
	 *
	 * <p>Errors: 400 if {@code password} breaks the rule (the link still works), or if the link is unknown,
	 * used, replaced by a newer one, expired, or for an account that can no longer sign in.
	 *
	 * @param request the token and the new password, a {@link ResetPasswordRequest}
	 */
	@PostMapping("/reset-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetPassword(@Valid @RequestBody ResetPasswordRequest request)
	{
		passwordResetService.resetPassword(request.token(), request.password());
	}

	/**
	 * Signs the caller out, ending this session.
	 *
	 * <p>{@code POST /api/v1/auth/logout}. Any signed-in user. Only this session ends; the user's other
	 * sessions stay signed in. Answers 200 with no body.
	 *
	 * @param request the HTTP request
	 * @param principal the signed-in caller
	 */
	@PostMapping("/logout")
	public void logout(HttpServletRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		if (principal != null)
			log.info("User {} signed out", principal.getUser().getId());
		SecurityContextHolder.clearContext();
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}

	/**
	 * Returns the signed-in user.
	 *
	 * <p>{@code GET /api/v1/auth/me}. Any signed-in user. The user is read afresh on every request, so a
	 * changed name or role shows at once. Like the answer to {@code /login}, it carries the user's
	 * {@code business_id}, which user records leave out.
	 *
	 * @param principal the signed-in caller
	 * @return 200 with the signed-in user
	 */
	@GetMapping("/me")
	public AuthUserResponse me(@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return AuthUserResponse.from(principal.getUser());
	}

	private void saveSignedIn(Authentication authentication, HttpServletRequest request, HttpServletResponse response)
	{
		// Rotate the id of any session that existed before, so an id planted or observed while anonymous
		// (session fixation), or one from before a password change, can't be reused.
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}
}
