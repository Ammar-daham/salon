package com.example.salon.dao;

import com.example.salon.model.Contact;

import java.util.List;

public interface ContactDao
{
    Long addContact(Contact contact);

    List<Contact> getContactsForBusiness(Long businessId);

    List<Contact> getAllContacts();

    Contact getContactById(int id);

    int updateContactById(int id, Contact contact);

    int deleteContactById(int id);
}
