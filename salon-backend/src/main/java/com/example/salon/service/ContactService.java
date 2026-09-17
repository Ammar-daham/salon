package com.example.salon.service;

import com.example.salon.dao.ContactDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Contact;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
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

    public Contact getContactById(int id, AuthenticatedUser caller) {
        Contact c = fetch(id);
        // A business's contact is part of its public listing (customers can browse every salon);
        // a personal contact is only visible to its owner or an admin.
        if (c.getBusinessId() == null) {
            AccessControl.requireSelfOrAdmin(caller, c.getUserId());
        }
        return c;
    }

    public void updateContactById(int id, Contact contact, AuthenticatedUser caller) {
        requireWritePermission(id, caller);
        int row = contactDao.updateContactById(id, contact);
        if (row == 0)
            throw new BaseException("Contact with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
    }

    public void delectContactById(int id, AuthenticatedUser caller) {
        requireWritePermission(id, caller);
        int row = contactDao.deleteContactById(id);
        if (row == 0)
            throw new BaseException("Contact with id " + id + " not found", "NOT_FOUNT", ErrorCode.NOT_FOUND.getStatus());
    }

    private void requireWritePermission(int id, AuthenticatedUser caller) {
        Contact c = fetch(id);
        // A business's contact can only be changed by an admin; a personal contact by its owner or an admin.
        if (c.getBusinessId() != null) {
            AccessControl.requireAdmin(caller);
        } else {
            AccessControl.requireSelfOrAdmin(caller, c.getUserId());
        }
    }

    private Contact fetch(int id) {
        try {
            return contactDao.getContactById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new BaseException("Contact with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
        }
    }
}
