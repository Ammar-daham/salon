package com.example.salon.dao;

import com.example.salon.model.User;

import java.util.List;

public interface UserDao {
    Long addUser(User user);

    List<User> getAllUsers();

    User getUserById(int id);

    long updateUserById(long id, User user);

    long deleteUserById(long id, User user);
}
