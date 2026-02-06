package com.example.salon.service;

import com.example.salon.dao.UserDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserDao userDao;

    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Transactional
    public User addUser(User user) {
        try {
            userDao.addUser(user);
        } catch (DuplicateKeyException ex) {
            if (ex.getMessage().contains("contacts_value_key"))
                throw new BaseException("Contact already exists.", "CONFLICT", ErrorCode.DUPLICATE_RESOURCE.getStatus());
        }
        return user;
    }

    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    public User getUserById(int id) {
        User user;
        try {
            user = userDao.getUserById(id);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("User with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
        }
        return user;
    }

    public void updateUserById(long id, User user) {
        long row = userDao.updateUserById(id, user);
        if (row == 0)
            throw new BaseException("User with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
    }

    public void deleteUserById(long id, User user) {
        long row = userDao.deleteUserById(id, user);
        if (row == 0)
            throw new BaseException("User with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
    }


}
