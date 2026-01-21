package com.example.salon.controller;

import com.example.salon.model.Business;
import com.example.salon.model.User;
import com.example.salon.service.BusinessService;
import com.example.salon.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequestMapping("api/v1/users")
@RestController
public class UserController
{
    private final UserService userService;

    @Autowired
    public UserController(UserService userService)
    {
        this.userService = userService;
    }

    @PostMapping
    public User addUser(@RequestBody User user)
    {
        User u = userService.addUser(user);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(user.getId())
                .toUri();

        return ResponseEntity.created(location).body(u).getBody();
    }

    @GetMapping
    public List<User> getUsers()
    {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable int id)
    {
        return userService.getUserById(id);
    }
}
