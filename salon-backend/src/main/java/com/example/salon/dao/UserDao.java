package com.example.salon.dao;

import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;

import java.util.List;
import java.util.Optional;

public interface UserDao
{
	enum SortBy { NAME, EMAIL, ROLE, CREATED_AT }

	/** search matches the name or email; role narrows to one role; businessId to one business, null for all. */
	record Filter(String search, Role role, Long businessId) {}

	Long addUser(User user);

	Page<User> getUsers(Filter filter, Sort<SortBy> sort, PageQuery page);

	User getUserById(long id);

	Optional<User> findById(long id);

	Optional<User> findByEmail(String email);

	/** The salon's users with the role who have an email, in the order they were added. */
	List<User> getUsersWithEmail(long businessId, Role role);

	long updateUserById(long id, User user);

	long updateUserRole(long id, Role role);

	long updatePasswordHash(long id, String passwordHash);

	long deleteUserById(long id);
}
