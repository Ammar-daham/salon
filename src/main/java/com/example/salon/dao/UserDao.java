package com.example.salon.dao;

import com.example.salon.model.User;

import java.util.List;

public interface UserDao
{
    Long addUser(User user);
    List<User> getAllUsers();
    User getUserById(int id);
    int updateUserById(User user);
    int deleteUserById(int id);
}
