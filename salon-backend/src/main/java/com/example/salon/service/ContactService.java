package com.example.salon.service;

import com.example.salon.dao.ContactDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService {

    private final ContactDao contactDao;

    @Autowired
    public ContactService(ContactDao contactDao) {
        this.contactDao = contactDao;
    }

    public List<Contact> getAllContacts() {
        return contactDao.getAllContacts();
    }

    public Contact getContactById(int id) {
        Contact c;
        try {
            c = contactDao.getContactById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new BaseException("Contact with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
        }
        return c;
    }

    public void updateContactById(int id, Contact contact) {
        int row = contactDao.updateContactById(id, contact);
        if (row == 0)
            throw new BaseException("Contact with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
    }

    public void delectContactById(int id) {
        int row = contactDao.deleteContactById(id);
        if (row == 0)
            throw new BaseException("Contact with id " + id + " not found", "NOT_FOUNT", ErrorCode.NOT_FOUND.getStatus());
    }
}
