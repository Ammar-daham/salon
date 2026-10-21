package com.example.salon.controller;

import com.example.salon.dto.ContactRequest;
import com.example.salon.model.Contact;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.ContactService;
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
 * The phone numbers, emails and other contacts of salons and users, under {@code /api/v1/contacts}.
 *
 * <p>A contact is created with the salon or user it belongs to ({@code POST /api/v1/businesses},
 * {@code POST /api/v1/users}) and deleted with them. A salon's can also be added later, with
 * {@code POST /api/v1/businesses/{businessId}/contacts}. Under {@code /api/v1/contacts} it is read,
 * replaced or deleted. A salon's contact may be read by anyone signed in and added or changed by an
 * ADMIN of that salon or a SUPER_ADMIN. A user's contact may be read and changed by the same people as
 * the user: that user, an ADMIN of their salon or a SUPER_ADMIN.
 */
@RestController
@RequestMapping("api/v1")
public class ContactController
{
	private final ContactService contactService;

	@Autowired
	public ContactController(ContactService contactService)
	{
		this.contactService = contactService;
	}

	/**
	 * Adds a contact to a salon.
	 *
	 * <p>{@code POST /api/v1/businesses/{businessId}/contacts}. An ADMIN of the salon, or a SUPER_ADMIN. The
	 * new contact is read, replaced and deleted under {@code /api/v1/contacts/{id}}, and comes in the salon's
	 * {@code contacts}.
	 *
	 * <p>Errors: 400 if the body breaks {@link ContactRequest}'s rules; 403 for anyone else, or another
	 * salon; 404 if the salon doesn't exist; 409 if the salon already has a contact with that
	 * {@code value}.
	 *
	 * @param businessId the salon's id
	 * @param request the contact, a {@link ContactRequest}
	 * @param principal the signed-in caller
	 * @return 201 Created with the contact as stored, and {@code /api/v1/contacts/{id}} in {@code Location}
	 */
	@PostMapping("/businesses/{businessId}/contacts")
	public ResponseEntity<Contact> addBusinessContact(@PathVariable long businessId,
			@Valid @RequestBody ContactRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		Contact contact = contactService.addContactForBusiness(businessId, request.toContact(), principal);
		URI location = ServletUriComponentsBuilder
				.fromCurrentContextPath()
				.path("/api/v1/contacts/{id}")
				.buildAndExpand(contact.getId())
				.toUri();

		return ResponseEntity.created(location).body(contact);
	}

	/**
	 * Lists the contacts the caller may see.
	 *
	 * <p>{@code GET /api/v1/contacts}. ADMIN and SUPER_ADMIN only. A SUPER_ADMIN gets every salon's and
	 * every user's contacts; an ADMIN gets their own salon's and its users'. In {@code id} order and not
	 * paged.
	 *
	 * <p>Errors: 403 for anyone else.
	 *
	 * @param principal the signed-in caller
	 * @return 200 with the contacts
	 */
	@GetMapping("/contacts")
	public List<Contact> getAllContacts(@AuthenticationPrincipal AuthenticatedUser principal)
	{
		return contactService.getAllContacts(principal);
	}

	/**
	 * Returns one contact.
	 *
	 * <p>{@code GET /api/v1/contacts/{id}}. A salon's contact, to anyone signed in; a user's, to that user,
	 * an ADMIN of their salon or a SUPER_ADMIN.
	 *
	 * <p>Errors: 404 if there's no such contact, or it's a user's and the caller may not see that user.
	 *
	 * @param id the contact's id
	 * @param principal the signed-in caller
	 * @return 200 with the contact
	 */
	@GetMapping("/contacts/{id}")
	public Contact getContactById(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return contactService.getContactById(id, principal);
	}

	/**
	 * Replaces a contact's type and value.
	 *
	 * <p>{@code PUT /api/v1/contacts/{id}}. A salon's contact, by an ADMIN of that salon or a SUPER_ADMIN; a
	 * user's, by that user, an ADMIN of their salon or a SUPER_ADMIN.
	 *
	 * <p>Errors: 400 if the body breaks {@link ContactRequest}'s rules; 403 if it's a salon's contact and the
	 * caller may not change it; 404 if there's no such contact, or it's a user's and the caller may not see
	 * that user; 409 if the owner already has another contact with this {@code value}.
	 *
	 * @param id the contact's id
	 * @param request the new type and value, a {@link ContactRequest}
	 * @param principal the signed-in caller
	 * @return 200 with the contact as stored
	 */
	@PutMapping("/contacts/{id}")
	public Contact updateContactById(@PathVariable long id, @Valid @RequestBody ContactRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return contactService.updateContactById(id, request.toContact(), principal);
	}

	/**
	 * Deletes a contact.
	 *
	 * <p>{@code DELETE /api/v1/contacts/{id}}. The same callers as {@code PUT /api/v1/contacts/{id}}.
	 * Answers 204.
	 *
	 * <p>Errors: 403 if it's a salon's contact and the caller may not change it; 404 if there's no such
	 * contact, or it's a user's and the caller may not see that user.
	 *
	 * @param id the contact's id
	 * @param principal the signed-in caller
	 */
	@DeleteMapping("/contacts/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteContactById(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		contactService.delectContactById(id, principal);
	}
}
