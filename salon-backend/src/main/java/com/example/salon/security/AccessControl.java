package com.example.salon.security;

import com.example.salon.dao.UserDao;
import com.example.salon.model.Role;
import com.example.salon.model.User;
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

	/**
	 * Changing a salon's own records, such as its address and contacts: an ADMIN of that business,
	 * or a SUPER_ADMIN. An EMPLOYEE of the business doesn't qualify.
	 */
	public static void requireAdminOfBusiness(AuthenticatedUser caller, long businessId)
	{
		if (!isAdmin(caller)) {
			throw new AccessDeniedException("You do not have permission to access this resource");
		}
		requireBusinessAccess(caller, businessId);
	}

	/**
	 * A SUPER_ADMIN may act on any business; anyone else only on the one they belong to.
	 * Shared by the business, service and opening-hours endpoints so tenant ownership is enforced identically.
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

	/**
	 * Staff work (customers and appointments): a SUPER_ADMIN anywhere, or an ADMIN or
	 * EMPLOYEE of that business. A CUSTOMER never qualifies, even with a business_id.
	 */
	public static void requireStaffOfBusiness(AuthenticatedUser caller, long businessId)
	{
		if (!isStaffOfBusiness(caller, businessId)) {
			throw new AccessDeniedException("You can only access your own business");
		}
	}

	public static boolean isStaffOfBusiness(AuthenticatedUser caller, long businessId)
	{
		if (isSuperAdmin(caller)) {
			return true;
		}
		Role role = caller.getUser().getRole();
		Long callerBusinessId = caller.getUser().getBusinessId();
		return (role == Role.ADMIN || role == Role.EMPLOYEE) && callerBusinessId != null && callerBusinessId == businessId;
	}

	/**
	 * Read/modify access to a single user record: a SUPER_ADMIN may touch anyone, a user may touch
	 * themselves, and an ADMIN may touch other users only within their own business. 
	 */
	public static void requireUserAccess(AuthenticatedUser caller, User target)
	{
		if (isSuperAdmin(caller) || isSelf(caller, target.getId())) {
			return;
		}
		Long callerBusinessId = caller.getUser().getBusinessId();
		if (isAdmin(caller) && callerBusinessId != null && callerBusinessId.equals(target.getBusinessId())) {
			return;
		}
		throw new AccessDeniedException("You do not have permission to access this resource");
	}

	/**
	 * {@link #requireUserAccess(AuthenticatedUser, User)} for a record that belongs to a user, such as
	 * their address or a contact: the same people may reach it as may reach the user.
	 */
	public static void requireUserAccess(AuthenticatedUser caller, long ownerId, UserDao userDao)
	{
		// An owner always exists: the foreign key deletes a user's addresses and contacts with them.
		User owner = userDao.findById(ownerId).orElseThrow();
		requireUserAccess(caller, owner);
	}
}
