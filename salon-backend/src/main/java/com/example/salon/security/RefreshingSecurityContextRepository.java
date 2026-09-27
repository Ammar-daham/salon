package com.example.salon.security;

import com.example.salon.dao.UserDao;
import com.example.salon.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Optional;

/**
 * Wraps the session-backed repository so the {@link AuthenticatedUser} stored at login is only
 * trusted for its id. On every request the user is re-read from the database: a changed role or
 * business takes effect immediately, and a deleted user (or one whose login was removed) has their
 * session invalidated instead of keeping their old access until it expires.
 */
public class RefreshingSecurityContextRepository implements SecurityContextRepository
{
	private final SecurityContextRepository delegate;
	private final UserDao userDao;

	public RefreshingSecurityContextRepository(SecurityContextRepository delegate, UserDao userDao)
	{
		this.delegate = delegate;
		this.userDao = userDao;
	}

	@Override
	public DeferredSecurityContext loadDeferredContext(HttpServletRequest request)
	{
		DeferredSecurityContext stored = delegate.loadDeferredContext(request);
		return new DeferredSecurityContext()
		{
			private SecurityContext refreshed;
			private boolean generated;

			@Override
			public SecurityContext get()
			{
				if (refreshed == null) {
					SecurityContext context = stored.get();
					refreshed = stored.isGenerated() ? context : refresh(context, request);
					generated = stored.isGenerated() || refreshed.getAuthentication() == null;
				}
				return refreshed;
			}

			@Override
			public boolean isGenerated()
			{
				get();
				return generated;
			}
		};
	}

	@Override
	@Deprecated
	public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder)
	{
		return loadDeferredContext(requestResponseHolder.getRequest()).get();
	}

	@Override
	public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response)
	{
		delegate.saveContext(context, request, response);
	}

	@Override
	public boolean containsContext(HttpServletRequest request)
	{
		return delegate.containsContext(request);
	}

	private SecurityContext refresh(SecurityContext context, HttpServletRequest request)
	{
		Authentication authentication = context.getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
			return context;
		}

		// Same rule as AppUserDetailsService: no password hash means the user can't sign in.
		Optional<User> current = userDao.findById(principal.getUser().getId())
				.filter(user -> user.getPasswordHash() != null);

		SecurityContext fresh = SecurityContextHolder.createEmptyContext();
		if (current.isEmpty()) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}
			return fresh;
		}

		AuthenticatedUser reloaded = new AuthenticatedUser(current.get());
		UsernamePasswordAuthenticationToken token =
				UsernamePasswordAuthenticationToken.authenticated(reloaded, null, reloaded.getAuthorities());
		token.setDetails(authentication.getDetails());
		fresh.setAuthentication(token);
		return fresh;
	}
}
