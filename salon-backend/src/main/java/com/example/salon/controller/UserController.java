package com.example.salon.controller;

import com.example.salon.dto.CreateUserRequest;
import com.example.salon.dto.UpdateUserRequest;
import com.example.salon.model.User;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.UserService;
import jakarta.validation.Valid;
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
	public ResponseEntity<User> addUser(@Valid @RequestBody CreateUserRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		User user = request.toUser();
		User u = userService.addUser(user, principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(user.getId())
				.toUri();

		return ResponseEntity.created(location).body(u);
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
	public void updateUserById(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		userService.updateUserById(id, request.toUser(), principal);
	}

	@DeleteMapping("/{id}")
	public void deleteUserById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		userService.deleteUserById(id, principal);
	}
}
