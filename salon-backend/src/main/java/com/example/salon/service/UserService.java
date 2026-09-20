package com.example.salon.service;

import com.example.salon.dao.UserDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService
{

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
			// A super admin is a platform-level role, not tied to one business, so they must say
			// which business the new user belongs to - and that business has to actually exist.
			if (user.getBusinessId() == null) {
				throw new BaseException("businessId is required when a super admin creates a user",
						"BAD_REQUEST", ErrorCode.BAD_REQUEST.getStatus());
			}
			businessService.getBusinessById(user.getBusinessId().intValue());
		} else {
			// Any other caller can only ever create users in their own business - the client's
			// businessId (if any) is ignored so an ADMIN can't attach a user to a business they
			// don't run.
			user.setBusinessId(caller.getUser().getBusinessId());
		}

		boolean canLogIn = user.getRole() != Role.CUSTOMER;
		if (canLogIn && (user.getEmail() == null || user.getEmail().isBlank()
				|| user.getPassword() == null || user.getPassword().isBlank())) {
			throw new BaseException("Email and password are required for role " + user.getRole(),
					"BAD_REQUEST", ErrorCode.BAD_REQUEST.getStatus());
		}
		if (user.getPassword() != null && !user.getPassword().isBlank()) {
			user.setPasswordHash(passwordEncoder.encode(user.getPassword()));
		}
		user.setPassword(null);

		try {
			userDao.addUser(user);
		} catch (DuplicateKeyException ex) {
			if (ex.getMessage().contains("contacts_value_key"))
				throw new BaseException("Contact already exists.", "CONFLICT", ErrorCode.DUPLICATE_RESOURCE.getStatus());
			if (ex.getMessage().contains("users_email_unique_idx"))
				throw new BaseException("Email already in use.", "CONFLICT", ErrorCode.DUPLICATE_RESOURCE.getStatus());
			throw ex;
		}
		return user;
	}

	public List<User> getAllUsers()
	{
		return userDao.getAllUsers();
	}

	public User getUserById(int id, AuthenticatedUser caller)
	{
		AccessControl.requireSelfOrAdmin(caller, (long) id);
		User user;
		try {
			user = userDao.getUserById(id);
		} catch (EmptyResultDataAccessException ex) {
			throw new BaseException("User with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
		}
		return user;
	}

	public void updateUserById(long id, User user, AuthenticatedUser caller)
	{
		AccessControl.requireSelfOrAdmin(caller, id);
		if (!AccessControl.isAdmin(caller)) {
			// Only an admin may change a user's role - a self-update must keep the caller's current one.
			User existing = getUserById((int) id, caller);
			user.setRole(existing.getRole());
		}
		long row = userDao.updateUserById(id, user);
		if (row == 0)
			throw new BaseException("User with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
	}

	public void deleteUserById(long id, User user, AuthenticatedUser caller)
	{
		AccessControl.requireSelfOrAdmin(caller, id);
		long row = userDao.deleteUserById(id, user);
		if (row == 0)
			throw new BaseException("User with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
	}


}
