package com.example.salon.controller;

import com.example.salon.model.User;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
	public User addUser(@RequestBody User user, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		User u = userService.addUser(user, principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(user.getId())
				.toUri();

		return ResponseEntity.created(location).body(u).getBody();
	}

	@GetMapping
	public List<User> getUsers(@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return userService.getAllUsers(principal);
	}

	@GetMapping("/{id}")
	public User getUserById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return userService.getUserById(id, principal);
	}

	@PutMapping("/{id}")
	public void updateUserById(@PathVariable long id, @RequestBody User user, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		userService.updateUserById(id, user, principal);
	}

	@DeleteMapping("/{id}")
	public void deleteUserById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		// BE-10: no request body - the server deletes the user's real children, not a client-sent object.
		userService.deleteUserById(id, principal);
	}
}
