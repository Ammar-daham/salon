package com.example.salon.dao;

import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;

import java.util.Optional;

public interface UserDao
{
	enum SortBy { NAME, EMAIL, ROLE, CREATED_AT }

	/** search matches the name or email; role narrows to one role; businessId to one business, null for all. */
	record Filter(String search, Role role, Long businessId) {}

	Long addUser(User user);

	Page<User> getUsers(Filter filter, Sort<SortBy> sort, PageQuery page);

	User getUserById(int id);

	Optional<User> findById(long id);

	Optional<User> findByEmail(String email);

	long updateUserById(long id, User user);

	long updatePasswordHash(long id, String passwordHash);

	long deleteUserById(long id);
}
