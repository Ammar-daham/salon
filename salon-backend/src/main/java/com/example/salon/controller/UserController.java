package com.example.salon.controller;

import com.example.salon.dao.UserDao;
import com.example.salon.dto.CreateUserRequest;
import com.example.salon.dto.UpdateUserRequest;
import com.example.salon.dto.UserRoleRequest;
import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
	 * in {@code business_id}, unless the new user is a SUPER_ADMIN too, who may belong to a salon or to
	 * none; an ADMIN's new user joins the ADMIN's own salon, whatever {@code business_id} says. Only a
	 * SUPER_ADMIN may create a SUPER_ADMIN. Every role but CUSTOMER needs an {@code email} and
	 * {@code password}; a CUSTOMER may have both, and can then sign in, for the customer app. The password
	 * is stored hashed and never sent back.
	 *
	 * <p>Errors: 400 if the body breaks {@link CreateUserRequest}'s rules, or a SUPER_ADMIN leaves out
	 * {@code business_id} for anyone but a SUPER_ADMIN; 403 for anyone else, or if an ADMIN asks for a
	 * SUPER_ADMIN; 404 if {@code business_id} is no salon; 409 if the email is already in use, in any
	 * case, or the same contact value is given twice.
	 *
	 * @param request the new user, a {@link CreateUserRequest}
	 * @param principal the signed-in caller
	 * @return 201 Created with the user as stored, and its address in {@code Location}
	 */
	@PostMapping
	public ResponseEntity<User> addUser(@Valid @RequestBody CreateUserRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		User user = request.toUser();
		User u = userService.addUser(user, principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(u.getId())
				.toUri();

		return ResponseEntity.created(location).body(u);
	}

	/**
	 * Lists the users the caller may see, a page at a time.
	 *
	 * <p>{@code GET /api/v1/users}. ADMIN and SUPER_ADMIN only: a SUPER_ADMIN sees every user, or one
	 * salon's with {@code business_id}, and an ADMIN those of their own salon, whether they name it or not.
	 * {@code q} searches the full name and the email, ignoring case; {@code sort} is {@code name} (first,
	 * then last name; the default), {@code email}, {@code role} or {@code created_at}. Each user comes with
	 * their addresses and contacts.
	 *
	 * <p>Errors: 400 if {@code business_id} or {@code role} isn't valid, or {@code page}, {@code size} or
	 * {@code sort} is out of range; 403 for anyone else, or for an ADMIN naming another salon; 404 if the
	 * {@code business_id} salon doesn't exist.
	 *
	 * @param businessId only this salon's users, or nothing for every user the caller may see
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
			@RequestParam(name = "business_id", required = false) Long businessId,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Role role,
			@RequestParam(defaultValue = "name") String sort,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return userService.getUsers(businessId, q, role, Sort.parse(sort, UserDao.SortBy.class),
				PageQuery.of(page, size), principal);
	}

	/**
	 * Returns one user, with their addresses and contacts.
	 *
	 * <p>{@code GET /api/v1/users/{id}}. A SUPER_ADMIN may read anyone, an ADMIN the users of their own
	 * salon, and anyone themselves.
	 *
	 * <p>Errors: 404 if there's no such user, or the caller may not see them.
	 *
	 * @param id the user's id
	 * @param principal the signed-in caller
	 * @return 200 with the user
	 */
	@GetMapping("/{id}")
	public User getUserById(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return userService.getUserById(id, principal);
	}

	/**
	 * Changes a user's name.
	 *
	 * <p>{@code PUT /api/v1/users/{id}}. The same callers as {@code GET /api/v1/users/{id}}, so a user may
	 * rename themselves. The role changes with {@code PUT /api/v1/users/{id}/role}, and a {@code role} in the
	 * body is ignored. The email, password and salon stay as they are.
	 *
	 * <p>Errors: 400 if the body breaks {@link UpdateUserRequest}'s rules; 404 if there's no such user, or
	 * the caller may not see them.
	 *
	 * @param id the user's id
	 * @param request the new name, an {@link UpdateUserRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the user as stored
	 */
	@PutMapping("/{id}")
	public User updateUserById(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return userService.updateUserById(id, request.toUser(), principal);
	}

	/**
	 * Gives a user another role.
	 *
	 * <p>{@code PUT /api/v1/users/{id}/role}. ADMIN and SUPER_ADMIN only, for a user they may see: an
	 * ADMIN, their own salon's users, themselves included. Only a SUPER_ADMIN may grant SUPER_ADMIN, or
	 * change the role of someone who has it. Asking for the role the user already has changes nothing. The
	 * new role takes effect from the user's next request, in sessions they already have too.
	 *
	 * <p>Errors: 400 if {@code role} is missing or isn't one of the {@link Role} names; 403 for anyone else,
	 * or if an ADMIN grants SUPER_ADMIN or changes a SUPER_ADMIN's role; 404 if there's no such user, or the
	 * caller may not see them.
	 *
	 * @param id the user's id
	 * @param request the new {@code role}: see {@link UserRoleRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the user as stored
	 */
	@PutMapping("/{id}/role")
	public User changeRole(@PathVariable long id, @Valid @RequestBody UserRoleRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return userService.changeRole(id, request.role(), principal);
	}

	/**
	 * Deletes a user, with their addresses and contacts.
	 *
	 * <p>{@code DELETE /api/v1/users/{id}}. The same callers as {@code GET /api/v1/users/{id}}, so a user
	 * may delete their own account. Their staff records go too, and every session of theirs is signed out.
	 * Answers 204.
	 *
	 * <p>Errors: 404 if there's no such user, or the caller may not see them; 409 if they are the staff
	 * member on any appointment.
	 *
	 * @param id the user's id
	 * @param principal the signed-in caller
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteUserById(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		userService.deleteUserById(id, principal);
	}
}
