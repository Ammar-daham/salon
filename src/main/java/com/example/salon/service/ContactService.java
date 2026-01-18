package com.example.salon.service;

import com.example.salon.dao.ContactDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ContactService {

    private final ContactDao contactDao;

    @Autowired
    public ContactService(ContactDao contactDao)
    {
        this.contactDao = contactDao;
    }

    public void delectContactById(int id)
    {
        int row = contactDao.deleteContactById(id);
        if (row == 0)
            throw new BaseException("Contact with id " + id + " not found", "NOT_FOUNT", ErrorCode.NOT_FOUND.getStatus());
    }
}
