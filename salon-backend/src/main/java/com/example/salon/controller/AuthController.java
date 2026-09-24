package com.example.salon.controller;

import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.security.AuthUserResponse;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.security.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;

	@Autowired
	public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository)
	{
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	@PostMapping("/login")
	public AuthUserResponse login(@RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse)
	{
		Authentication authResult;
		try {
			authResult = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password())
			);
		} catch (AuthenticationException ex) {
			throw new BaseException("Invalid email or password", ErrorCode.UNAUTHORIZED);
		}

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authResult);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, servletRequest, servletResponse);

		return AuthUserResponse.from(((AuthenticatedUser) authResult.getPrincipal()).getUser());
	}

	@PostMapping("/logout")
	public void logout(HttpServletRequest request)
	{
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
