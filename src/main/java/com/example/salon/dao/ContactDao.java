package com.example.salon.dao;

import com.example.salon.model.Contact;

import java.util.List;

public interface ContactDao
{
    void addContactForBusiness(Long businessId, Contact contact);

    void AddContactsForBusiness(Long businessId, List<Contact> contacts);
}
