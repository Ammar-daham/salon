package com.example.salon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

/**
 * Refuses a request that didn't come over HTTPS (BE-25), when app.require-https is on, as under the
 * prod profile. Behind the proxy that terminates TLS, server.forward-headers-strategy makes a request
 * that reached the proxy over HTTPS a secure one here. One that isn't means a sign-in's password or the
 * session cookie may have crossed the network in the clear, or the proxy isn't sending, or isn't
 * trusted to send, X-Forwarded-Proto; either way it should fail loudly rather than work insecurely.
 * The health check is let through, since a load balancer probes the app directly.
 */
final class RequireHttpsFilter extends OncePerRequestFilter
{
	private static final RequestMatcher HEALTH_CHECK = pathPattern(HttpMethod.GET, "/actuator/health/**");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException
	{
		if (request.isSecure() || HEALTH_CHECK.matches(request)) {
			chain.doFilter(request, response);
			return;
		}
		SecurityResponseWriter.writeError(response, request, HttpStatus.FORBIDDEN, "FORBIDDEN",
				"This API only answers over HTTPS");
	}
}
