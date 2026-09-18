package com.example.salon.dao;

import com.example.salon.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao
{
	Long addUser(User user);

	List<User> getAllUsers();

	User getUserById(int id);

	Optional<User> findByEmail(String email);

	long updateUserById(long id, User user);

	long deleteUserById(long id, User user);
}
