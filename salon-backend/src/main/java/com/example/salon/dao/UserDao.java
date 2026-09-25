package com.example.salon.dao;

import com.example.salon.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao
{
	Long addUser(User user);

	List<User> getAllUsers();

	List<User> getUsersByBusinessId(long businessId);

	User getUserById(int id);

	/** The user row alone, without addresses or contacts. */
	Optional<User> findById(long id);

	Optional<User> findByEmail(String email);

	long updateUserById(long id, User user);

	long deleteUserById(long id);
}
