package com.example.salon.controller;

import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.logging.RequestLog;
import com.example.salon.model.User;
import com.example.salon.security.AuthUserResponse;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.security.LoginRequest;
import com.example.salon.security.LoginThrottle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("api/v1/auth")
@RestController
public class AuthController
{
	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final LoginThrottle loginThrottle;

	@Autowired
	public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository,
			LoginThrottle loginThrottle)
	{
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.loginThrottle = loginThrottle;
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

		// Rotate the id of any session that existed before login, so an id planted or observed
		// while anonymous can't be reused as this user's authenticated session (session fixation).
		if (servletRequest.getSession(false) != null) {
			servletRequest.changeSessionId();
		}

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authResult);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, servletRequest, servletResponse);

		User user = ((AuthenticatedUser) authResult.getPrincipal()).getUser();
		log.info("User {} signed in", user.getId());
		return AuthUserResponse.from(user);
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
}
