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

/**
 * User accounts, under {@code /api/v1/users}: the platform's SUPER_ADMINs, each salon's ADMINs and
 * EMPLOYEEs, and CUSTOMERs.
 *
 * <p>Listing and creating users is for an ADMIN or SUPER_ADMIN. One user may be read, changed or deleted
 * by a SUPER_ADMIN, by an ADMIN of the user's salon, or by the user themselves. A user record never
 * carries the password or {@code business_id}.
 */
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

	/**
	 * Creates a user, with their addresses and contacts.
	 *
	 * <p>{@code POST /api/v1/users}. ADMIN and SUPER_ADMIN only. A SUPER_ADMIN must name the user's salon
	 * in {@code business_id}; an ADMIN's new user joins the ADMIN's own salon, whatever
	 * {@code business_id} says. Only a SUPER_ADMIN may create a SUPER_ADMIN. Every role but CUSTOMER needs
	 * an {@code email} and {@code password}; the password is stored hashed and never sent back.
	 *
	 * <p>Errors: 400 if the body breaks {@link CreateUserRequest}'s rules, or a SUPER_ADMIN leaves out
	 * {@code business_id}; 403 for anyone else, or if an ADMIN asks for a SUPER_ADMIN; 404 if
	 * {@code business_id} is no salon; 409 if the email is already in use, in any case, or the same
	 * contact value is given twice.
	 *
	 * @param request the new user, a {@link CreateUserRequest}
	 * @param principal the signed-in caller
	 * @return 201 Created with the user, and its address in {@code Location}
	 */
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

	/**
	 * Lists the users the caller may see, a page at a time.
	 *
	 * <p>{@code GET /api/v1/users}. ADMIN and SUPER_ADMIN only: a SUPER_ADMIN sees every user, an ADMIN
	 * those of their own salon. {@code q} searches the full name and the email, ignoring case;
	 * {@code sort} is {@code name} (first, then last name; the default), {@code email}, {@code role} or
	 * {@code created_at}. Each user comes with their addresses and contacts.
	 *
	 * <p>Errors: 400 if {@code role} isn't a role, or {@code page}, {@code size} or {@code sort} is out of
	 * range; 403 for anyone else.
	 *
	 * @param q text to search for, or nothing for every user
	 * @param role only users with this role, such as {@code EMPLOYEE}
	 * @param sort the order: a key above, or {@code -key} for descending
	 * @param page the page, counting from 1
	 * @param size users per page, 1 to 100, 20 if left out
	 * @param principal the signed-in caller
	 * @return 200 with one page of users
	 */
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

	/**
	 * Returns one user, with their addresses and contacts.
	 *
	 * <p>{@code GET /api/v1/users/{id}}. A SUPER_ADMIN may read anyone, an ADMIN the users of their own
	 * salon, and anyone themselves.
	 *
	 * <p>Errors: 403 if the caller may not see the user; 404 if there's no such user.
	 *
	 * @param id the user's id
	 * @param principal the signed-in caller
	 * @return 200 with the user
	 */
	@GetMapping("/{id}")
	public User getUserById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return userService.getUserById(id, principal);
	}

	/**
	 * Changes a user's name and role.
	 *
	 * <p>{@code PUT /api/v1/users/{id}}. The same callers as {@code GET /api/v1/users/{id}}. Only an ADMIN
	 * or SUPER_ADMIN changes the role; anyone else must still send one, but keeps their own. Only a
	 * SUPER_ADMIN may grant SUPER_ADMIN. The email, password and salon stay as they are. A new role takes
	 * effect from the user's next request, in sessions they already have too. Answers 200 with no body.
	 *
	 * <p>Errors: 400 if the body breaks {@link UpdateUserRequest}'s rules; 403 if the caller may not change
	 * the user, or if an ADMIN grants SUPER_ADMIN; 404 if there's no such user.
	 *
	 * @param id the user's id
	 * @param request the new name and role, an {@link UpdateUserRequest}
	 * @param principal the signed-in caller
	 */
	@PutMapping("/{id}")
	public void updateUserById(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		userService.updateUserById(id, request.toUser(), principal);
	}

	/**
	 * Deletes a user, with their addresses and contacts.
	 *
	 * <p>{@code DELETE /api/v1/users/{id}}. The same callers as {@code GET /api/v1/users/{id}}, so a user
	 * may delete their own account. Their staff records go too, and every session of theirs is signed out.
	 * Answers 200 with no body.
	 *
	 * <p>Errors: 403 if the caller may not delete the user; 404 if there's no such user; 409 if they are
	 * the staff member on any appointment.
	 *
	 * @param id the user's id
	 * @param principal the signed-in caller
	 */
	@DeleteMapping("/{id}")
	public void deleteUserById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		userService.deleteUserById(id, principal);
	}
}
