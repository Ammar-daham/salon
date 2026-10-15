package com.example.salon.controller;

import com.example.salon.dto.AddressRequest;
import com.example.salon.model.Address;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The addresses of salons and users, under {@code /api/v1/addresses}.
 *
 * <p>An address is created with the salon or user it belongs to ({@code POST /api/v1/businesses},
 * {@code POST /api/v1/users}) and deleted with them; here it is read, replaced or deleted. A salon's
 * address may be read by anyone signed in and changed by an ADMIN of that salon or a SUPER_ADMIN. A
 * user's address may be read and changed by the same people as the user: that user, an ADMIN of their
 * salon or a SUPER_ADMIN.
 */
@RestController
@RequestMapping("api/v1/addresses")
public class AddressController
{
	private final AddressService addressService;

	@Autowired
	public AddressController(AddressService addressService)
	{
		this.addressService = addressService;
	}

	/**
	 * Lists the addresses the caller may see.
	 *
	 * <p>{@code GET /api/v1/addresses}. ADMIN and SUPER_ADMIN only. A SUPER_ADMIN gets every salon's and
	 * every user's addresses; an ADMIN gets their own salon's and its users'. In {@code id} order and not
	 * paged. {@code user_id} is {@code null} throughout.
	 *
	 * <p>Errors: 403 for anyone else.
	 *
	 * @param principal the signed-in caller
	 * @return 200 with the addresses
	 */
	@GetMapping
	public List<Address> getAllAddresses(@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return addressService.getAllAddresses(principal);
	}

	/**
	 * Returns one address.
	 *
	 * <p>{@code GET /api/v1/addresses/{id}}. A salon's address, to anyone signed in; a user's, to that user,
	 * an ADMIN of their salon or a SUPER_ADMIN. For a user's address, {@code user_id} names the user.
	 *
	 * <p>Errors: 403 if it's a user's address and the caller may not see that user; 404 if there's no such
	 * address.
	 *
	 * @param id the address's id
	 * @param principal the signed-in caller
	 * @return 200 with the address
	 */
	@GetMapping("/{id}")
	public Address getAddress(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return addressService.getAddressById(id, principal);
	}

	/**
	 * Replaces an address.
	 *
	 * <p>{@code PUT /api/v1/addresses/{id}}. A salon's address, by an ADMIN of that salon or a SUPER_ADMIN;
	 * a user's, by that user, an ADMIN of their salon or a SUPER_ADMIN. Every field is replaced, so an
	 * optional one left out is cleared. Answers 200 with no body.
	 *
	 * <p>Errors: 400 if the body breaks {@link AddressRequest}'s rules; 403 if the caller may not change
	 * the address; 404 if there's no such address.
	 *
	 * @param id the address's id
	 * @param request the new address, an {@link AddressRequest}
	 * @param principal the signed-in caller
	 */
	@PutMapping("/{id}")
	public void updateAddress(@PathVariable int id, @Valid @RequestBody AddressRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		addressService.updateAddressById(id, request.toAddress(), principal);
	}

	/**
	 * Deletes an address.
	 *
	 * <p>{@code DELETE /api/v1/addresses/{id}}. The same callers as {@code PUT /api/v1/addresses/{id}}.
	 * Answers 200 with no body.
	 *
	 * <p>Errors: 403 if the caller may not change the address; 404 if there's no such address.
	 *
	 * @param id the address's id
	 * @param principal the signed-in caller
	 */
	@DeleteMapping("/{id}")
	public void deleteAddress(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		addressService.deleteAddressById(id, principal);
	}


}
