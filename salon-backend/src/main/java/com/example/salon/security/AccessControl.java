package com.example.salon.security;

import com.example.salon.model.Role;
import org.springframework.security.access.AccessDeniedException;

public final class AccessControl
{
	private AccessControl()
	{
	}

	public static boolean isAdmin(AuthenticatedUser caller)
	{
		Role role = caller.getUser().getRole();
		return role == Role.ADMIN || role == Role.SUPER_ADMIN;
	}

	public static boolean isSuperAdmin(AuthenticatedUser caller)
	{
		return caller.getUser().getRole() == Role.SUPER_ADMIN;
	}

	public static boolean isSelf(AuthenticatedUser caller, Long ownerId)
	{
		return ownerId != null && ownerId.equals(caller.getUser().getId());
	}

	public static void requireSelfOrAdmin(AuthenticatedUser caller, Long ownerId)
	{
		if (isAdmin(caller) || isSelf(caller, ownerId)) {
			return;
		}
		throw new AccessDeniedException("You do not have permission to access this resource");
	}

	public static void requireAdmin(AuthenticatedUser caller)
	{
		if (!isAdmin(caller)) {
			throw new AccessDeniedException("You do not have permission to access this resource");
		}
	}

	/**
	 * A SUPER_ADMIN may act on any business; anyone else only on the one they belong to.
	 * Shared by the business and service endpoints so tenant ownership is enforced identically.
	 */
	public static void requireBusinessAccess(AuthenticatedUser caller, long businessId)
	{
		if (isSuperAdmin(caller)) {
			return;
		}
		Long callerBusinessId = caller.getUser().getBusinessId();
		if (callerBusinessId == null || callerBusinessId != businessId) {
			throw new AccessDeniedException("You can only modify your own business");
		}
	}
}
