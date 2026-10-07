package com.example.salon.controller;

import com.example.salon.dto.ChangePasswordRequest;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.logging.RequestLog;
import com.example.salon.model.User;
import com.example.salon.security.AuthUserResponse;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.security.LoginRequest;
import com.example.salon.security.LoginThrottle;
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

	@Autowired
	public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
			LoginThrottle loginThrottle, PasswordEncoder passwordEncoder, UserService userService)
	{
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.loginThrottle = loginThrottle;
		this.passwordEncoder = passwordEncoder;
		this.userService = userService;
	}

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
	 * The signed-in user's own password (FE-13). Every other session of theirs is signed out; this
	 * one stays signed in, under a new id.
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
