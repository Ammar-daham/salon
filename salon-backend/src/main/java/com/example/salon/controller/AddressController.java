package com.example.salon.controller;

import com.example.salon.dto.AddressRequest;
import com.example.salon.model.Address;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * The addresses of salons and users, under {@code /api/v1/addresses}.
 *
 * <p>An address is created with the salon or user it belongs to ({@code POST /api/v1/businesses},
 * {@code POST /api/v1/users}) and deleted with them. A salon's can also be added later, with
 * {@code POST /api/v1/businesses/{businessId}/addresses}. Under {@code /api/v1/addresses} it is read,
 * replaced or deleted. A salon's address may be read by anyone signed in and added or changed by an
 * ADMIN of that salon or a SUPER_ADMIN. A user's address may be read and changed by the same people as
 * the user: that user, an ADMIN of their salon or a SUPER_ADMIN.
 */
@RestController
@RequestMapping("api/v1")
public class AddressController
{
	private final AddressService addressService;

	@Autowired
	public AddressController(AddressService addressService)
	{
		this.addressService = addressService;
	}

	/**
	 * Adds an address to a salon.
	 *
	 * <p>{@code POST /api/v1/businesses/{businessId}/addresses}. An ADMIN of the salon, or a SUPER_ADMIN. A
	 * salon may have any number of addresses. The new one is read, replaced and deleted under
	 * {@code /api/v1/addresses/{id}}, and comes in the salon's {@code addresses}.
	 *
	 * <p>Errors: 400 if the body breaks {@link AddressRequest}'s rules; 403 for anyone else, or another
	 * salon; 404 if the salon doesn't exist.
	 *
	 * @param businessId the salon's id
	 * @param request the address, an {@link AddressRequest}
	 * @param principal the signed-in caller
	 * @return 201 Created with the address as stored, and {@code /api/v1/addresses/{id}} in {@code Location}
	 */
	@PostMapping("/businesses/{businessId}/addresses")
	public ResponseEntity<Address> addBusinessAddress(@PathVariable long businessId,
			@Valid @RequestBody AddressRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		Address address = addressService.addAddressForBusiness(businessId, request.toAddress(), principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentContextPath()
				.path("/api/v1/addresses/{id}")
				.buildAndExpand(address.getId())
				.toUri();

		return ResponseEntity.created(location).body(address);
	}

	/**
	 * Lists the addresses the caller may see.
	 *
	 * <p>{@code GET /api/v1/addresses}. ADMIN and SUPER_ADMIN only. A SUPER_ADMIN gets every salon's and
	 * every user's addresses; an ADMIN gets their own salon's and its users'. In {@code id} order and not
	 * paged. {@code user_id} names the user a user's address belongs to, and is {@code null} for a salon's.
	 *
	 * <p>Errors: 403 for anyone else.
	 *
	 * @param principal the signed-in caller
	 * @return 200 with the addresses
	 */
	@GetMapping("/addresses")
	public List<Address> getAllAddresses(@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return addressService.getAllAddresses(principal);
	}

	/**
	 * Returns one address.
	 *
	 * <p>{@code GET /api/v1/addresses/{id}}. A salon's address, to anyone signed in; a user's, to that user,
	 * an ADMIN of their salon or a SUPER_ADMIN.
	 *
	 * <p>Errors: 404 if there's no such address, or it's a user's and the caller may not see that user.
	 *
	 * @param id the address's id
	 * @param principal the signed-in caller
	 * @return 200 with the address
	 */
	@GetMapping("/addresses/{id}")
	public Address getAddress(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return addressService.getAddressById(id, principal);
	}

	/**
	 * Replaces an address.
	 *
	 * <p>{@code PUT /api/v1/addresses/{id}}. A salon's address, by an ADMIN of that salon or a SUPER_ADMIN;
	 * a user's, by that user, an ADMIN of their salon or a SUPER_ADMIN. Every field is replaced, so an
	 * optional one left out is cleared.
	 *
	 * <p>Errors: 400 if the body breaks {@link AddressRequest}'s rules; 403 if it's a salon's address and
	 * the caller may not change it; 404 if there's no such address, or it's a user's and the caller may
	 * not see that user.
	 *
	 * @param id the address's id
	 * @param request the new address, an {@link AddressRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the address as stored
	 */
	@PutMapping("/addresses/{id}")
	public Address updateAddress(@PathVariable int id, @Valid @RequestBody AddressRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return addressService.updateAddressById(id, request.toAddress(), principal);
	}

	/**
	 * Deletes an address.
	 *
	 * <p>{@code DELETE /api/v1/addresses/{id}}. The same callers as {@code PUT /api/v1/addresses/{id}}.
	 * Answers 204.
	 *
	 * <p>Errors: 403 if it's a salon's address and the caller may not change it; 404 if there's no such
	 * address, or it's a user's and the caller may not see that user.
	 *
	 * @param id the address's id
	 * @param principal the signed-in caller
	 */
	@DeleteMapping("/addresses/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteAddress(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		addressService.deleteAddressById(id, principal);
	}


}
