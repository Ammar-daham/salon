package com.example.salon.logging;

import com.example.salon.model.User;
import com.example.salon.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tells {@link RequestLoggingFilter} who made the request. It sits inside the security chain,
 * right after the security context is loaded, because by the time the outer filter writes its
 * line the context has already been cleared. It looks after the request, so a login is logged as
 * the user who just signed in, and falls back to before it, so a logout is logged as who left.
 */
public class CallerLoggingFilter extends OncePerRequestFilter
{
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException
	{
		String before = currentCaller();
		try {
			chain.doFilter(request, response);
		} finally {
			String after = currentCaller();
			String caller = after != null ? after : before;
			if (caller != null) {
				request.setAttribute(RequestLog.CALLER_ATTRIBUTE, caller);
			}
		}
	}

	private static String currentCaller()
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
			User user = principal.getUser();
			return user.getId() + " " + user.getRole();
		}
		return null;
	}
}
