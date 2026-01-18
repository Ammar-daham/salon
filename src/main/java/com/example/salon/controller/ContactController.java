package com.example.salon.controller;

import com.example.salon.model.Contact;
import com.example.salon.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @DeleteMapping("/{id}")
    public void deleteContactById(@PathVariable int id)
    {
        contactService.delectContactById(id);
    }
}
