package com.example.salon.controller;

import com.example.salon.dao.UserDao;
import com.example.salon.dto.CreateUserRequest;
import com.example.salon.dto.UpdateUserRequest;
import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

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

	/** A page of users (BE-15). q searches the name and email; sort is name, email, role or created_at. */
	@GetMapping
	public Page<User> getUsers(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Role role,
			@RequestParam(defaultValue = "name") String sort,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return userService.getUsers(q, role, Sort.parse(sort, UserDao.SortBy.class), PageQuery.of(page, size),
				principal);
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
