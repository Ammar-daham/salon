package com.example.salon.controller;

import com.example.salon.dto.ContactRequest;
import com.example.salon.model.Contact;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The phone numbers, emails and other contacts of salons and users, under {@code /api/v1/contacts}.
 *
 * <p>A contact is created with the salon or user it belongs to ({@code POST /api/v1/businesses},
 * {@code POST /api/v1/users}) and deleted with them; here it is read, replaced or deleted. A salon's
 * contact may be read by anyone signed in and changed by any ADMIN or SUPER_ADMIN, of whichever salon. A
 * user's contact may be read and changed by that user or by any ADMIN or SUPER_ADMIN.
 */
@RestController
@RequestMapping("api/v1/contacts")
public class ContactController
{
	private final ContactService contactService;

	@Autowired
	public ContactController(ContactService contactService)
	{
		this.contactService = contactService;
	}

	/**
	 * Lists every contact on the platform.
	 *
	 * <p>{@code GET /api/v1/contacts}. ADMIN and SUPER_ADMIN only. The list holds every salon's and every
	 * user's contacts, other salons' too for an ADMIN, in {@code id} order and not paged.
	 *
	 * <p>Errors: 403 for anyone else.
	 *
	 * @return 200 with every contact
	 */
	@GetMapping
	public List<Contact> getAllContacts()
	{
		return contactService.getAllContacts();
	}

	/**
	 * Returns one contact.
	 *
	 * <p>{@code GET /api/v1/contacts/{id}}. A salon's contact, to anyone signed in; a user's, to that user
	 * or any ADMIN or SUPER_ADMIN.
	 *
	 * <p>Errors: 403 if it's another user's contact and the caller isn't an ADMIN or SUPER_ADMIN; 404 if
	 * there's no such contact.
	 *
	 * @param id the contact's id
	 * @param principal the signed-in caller
	 * @return 200 with the contact
	 */
	@GetMapping("/{id}")
	public Contact getContactById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return contactService.getContactById(id, principal);
	}

	/**
	 * Replaces a contact's type and value.
	 *
	 * <p>{@code PUT /api/v1/contacts/{id}}. A salon's contact, by any ADMIN or SUPER_ADMIN; a user's, by
	 * that user or any ADMIN or SUPER_ADMIN. Answers 200 with no body.
	 *
	 * <p>Errors: 400 if the body breaks {@link ContactRequest}'s rules, or the owner already has another
	 * contact with this {@code value}; 403 if the caller may not change the contact; 404 if there's no
	 * such contact.
	 *
	 * @param id the contact's id
	 * @param request the new type and value, a {@link ContactRequest}
	 * @param principal the signed-in caller
	 */
	@PutMapping("/{id}")
	public void updateContactById(@PathVariable int id, @Valid @RequestBody ContactRequest request, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		contactService.updateContactById(id, request.toContact(), principal);
	}

	/**
	 * Deletes a contact.
	 *
	 * <p>{@code DELETE /api/v1/contacts/{id}}. The same callers as {@code PUT /api/v1/contacts/{id}}.
	 * Answers 200 with no body.
	 *
	 * <p>Errors: 403 if the caller may not change the contact; 404 if there's no such contact.
	 *
	 * @param id the contact's id
	 * @param principal the signed-in caller
	 */
	@DeleteMapping("/{id}")
	public void deleteContactById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		contactService.delectContactById(id, principal);
	}
}
