package com.example.salon.controller;

import com.example.salon.model.Contact;
import com.example.salon.security.AuthenticatedUser;
import com.example.salon.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

	@GetMapping
	public List<Contact> getAllContacts()
	{
		return contactService.getAllContacts();
	}

	@GetMapping("/{id}")
	public Contact getContactById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		return contactService.getContactById(id, principal);
	}

	@PutMapping("/{id}")
	public void updateContactById(@PathVariable int id, @RequestBody Contact contact, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		contactService.updateContactById(id, contact, principal);
	}

	@DeleteMapping("/{id}")
	public void deleteContactById(@PathVariable int id, @AuthenticationPrincipal AuthenticatedUser principal)
	{
		contactService.delectContactById(id, principal);
	}
}
