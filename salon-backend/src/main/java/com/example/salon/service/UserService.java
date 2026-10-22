package com.example.salon.service;

import com.example.salon.dao.Constraints;
import com.example.salon.dao.UserDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UserService
{
	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserDao userDao;
	private final PasswordEncoder passwordEncoder;
	private final BusinessService businessService;

	@Autowired
	public UserService(UserDao userDao, PasswordEncoder passwordEncoder, BusinessService businessService)
	{
		this.userDao = userDao;
		this.passwordEncoder = passwordEncoder;
		this.businessService = businessService;
	}

	@Transactional
	public User addUser(User user, AuthenticatedUser caller)
	{
		Role callerRole = caller.getUser().getRole();

		// A caller can never grant a role higher than their own - otherwise an ADMIN could mint
		// a SUPER_ADMIN.
		if (user.getRole() == Role.SUPER_ADMIN && callerRole != Role.SUPER_ADMIN) {
			throw new AccessDeniedException("Only a super admin can create another super admin");
		}

		if (callerRole == Role.SUPER_ADMIN) {
			// A super admin isn't tied to one business, so they must say which business the new user
			// belongs to, and it has to exist. A new super admin is platform-level too, and needn't
			// belong to one.
			if (user.getBusinessId() == null && user.getRole() != Role.SUPER_ADMIN) {
				throw new BaseException("business_id is required for role " + user.getRole(),
						ErrorCode.BAD_REQUEST);
			}
			if (user.getBusinessId() != null)
				businessService.getBusinessById(user.getBusinessId());
		} else {
			// Any other caller can only ever create users in their own business - the client's
			// businessId (if any) is ignored so an ADMIN can't attach a user to a business they
			// don't run.
			user.setBusinessId(caller.getUser().getBusinessId());
		}

		// Every other role works in the admin panel, so has to sign in. A CUSTOMER may have both, and then
		// can sign in too, for the customer app.
		boolean needsCredentials = user.getRole() != Role.CUSTOMER;
		if (needsCredentials && (user.getEmail() == null || user.getEmail().isBlank()
				|| user.getPassword() == null || user.getPassword().isBlank())) {
			throw new BaseException("Email and password are required for role " + user.getRole(),
					ErrorCode.BAD_REQUEST);
		}
		if (user.getPassword() != null && !user.getPassword().isBlank()) {
			user.setPasswordHash(passwordEncoder.encode(user.getPassword()));
		}
		user.setPassword(null);

		Long id;
		try {
			id = userDao.addUser(user);
		} catch (DuplicateKeyException ex) {
			if (Constraints.violated(ex, "contacts_user_value_unique_idx"))
				throw new BaseException("Contact already exists.", ErrorCode.DUPLICATE_RESOURCE);
			if (Constraints.violated(ex, "users_email_lower_unique_idx"))
				throw new BaseException("Email already in use.", ErrorCode.DUPLICATE_RESOURCE);
			throw ex;
		}
		log.info("Created user {} with role {} in business {}", id, user.getRole(), user.getBusinessId());
		return userDao.getUserById(id);
	}

	/**
	 * Every user for a super admin, or one salon's with businessId; for an admin, their own business's,
	 * whether they name it or not, or none without one. Naming another salon is refused, and naming one
	 * that doesn't exist is a 404, as on GET /customers.
	 */
	public Page<User> getUsers(Long businessId, String search, Role role, Sort<UserDao.SortBy> sort,
			PageQuery page, AuthenticatedUser caller)
	{
		Long scope = businessId;
		if (!AccessControl.isSuperAdmin(caller)) {
			Long own = caller.getUser().getBusinessId();
			if (businessId != null && !businessId.equals(own))
				throw new AccessDeniedException("You can only access your own business");
			if (own == null)
				return Page.empty(page);
			scope = own;
		}
		if (businessId != null)
			businessService.getBusinessById(businessId);
		return userDao.getUsers(new UserDao.Filter(search, role, scope), sort, page);
	}

	public User getUserById(long id, AuthenticatedUser caller)
	{
		User user;
		try {
			user = userDao.getUserById(id);
		} catch (EmptyResultDataAccessException ex) {
			throw new BaseException("User with id " + id + " not found", ErrorCode.NOT_FOUND);
		}
		// enforce ownership against the record we actually loaded, not just the caller's role. A user the
		// caller may not see is a 404, like one that doesn't exist, so ids can't be probed.
		if (!AccessControl.canAccessUser(caller, user))
			throw new BaseException("User with id " + id + " not found", ErrorCode.NOT_FOUND);
		return user;
	}

	@Transactional
	public User updateUserById(long id, User user, AuthenticatedUser caller)
	{
		// getUserById 404s unless the user exists and the caller may see them: self, own business or super admin.
		getUserById(id, caller);
		long row = userDao.updateUserById(id, user);
		if (row == 0)
			throw new BaseException("User with id " + id + " not found", ErrorCode.NOT_FOUND);
		log.info("Updated user {}", id);
		return getUserById(id, caller);
	}

	/**
	 * Only an admin changes a role, and only of a user they may see. A caller can never grant a role higher
	 * than their own, nor take it from someone who has it. Asking for the role the user already has changes
	 * nothing.
	 */
	@Transactional
	public User changeRole(long id, Role role, AuthenticatedUser caller)
	{
		if (!AccessControl.isAdmin(caller))
			throw new AccessDeniedException("Only an admin can change a user's role");
		// getUserById 404s unless the user exists and the caller may see them: own business or super admin.
		User existing = getUserById(id, caller);
		boolean touchesSuperAdmin = role == Role.SUPER_ADMIN || existing.getRole() == Role.SUPER_ADMIN;
		if (touchesSuperAdmin && !AccessControl.isSuperAdmin(caller))
			throw new AccessDeniedException("Only a super admin can grant or take away the super admin role");
		if (role != existing.getRole()) {
			userDao.updateUserRole(id, role);
			log.info("User {} role changed from {} to {}", id, existing.getRole(), role);
		}
		return getUserById(id, caller);
	}

	@Transactional
	public void deleteUserById(long id, AuthenticatedUser caller)
	{
		// getUserById 404s unless the user exists and the caller may see them: self, own business or super admin.
		getUserById(id, caller);
		long row;
		try {
			row = userDao.deleteUserById(id);
		} catch (DataIntegrityViolationException ex) {
			if (!Constraints.violated(ex, "fk_appointments_staff"))
				throw ex;
			// Their staff record would go with the account (fk_staff_user), and appointments point at it.
			throw new BaseException("User with id " + id + " is the staff member on appointments, "
					+ "so the account can't be deleted.", ErrorCode.DUPLICATE_RESOURCE);
		}
		if (row == 0)
			throw new BaseException("User with id " + id + " not found", ErrorCode.NOT_FOUND);
		log.info("Deleted user {}", id);
	}

	/**
	 * Sets a password the caller has already checked the user may set, and returns the user as stored.
	 * Every session that signed in with the old one is signed out (RefreshingSecurityContextRepository).
	 */
	public User setPassword(long id, String password)
	{
		userDao.updatePasswordHash(id, passwordEncoder.encode(password));
		return userDao.findById(id)
				.orElseThrow(() -> new BaseException("User with id " + id + " not found", ErrorCode.NOT_FOUND));
	}


}
