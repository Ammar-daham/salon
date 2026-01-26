package com.example.salon.service;

import com.example.salon.dao.BusinessDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Business;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessService {
    private final BusinessDao businessDao;

    @Autowired
    public BusinessService(BusinessDao businessDao) {
        this.businessDao = businessDao;
    }

    @Transactional
    public Business addBusiness(Business business) {
        try {
            businessDao.addBusiness(business);
        } catch (DuplicateKeyException ex) {
            System.out.println("ex.getMessage() " + ex.getMessage());
            if (ex.getMessage().contains("businesses_name_unique"))
                throw new BaseException("Business with name " + business.getName() + " already exists.", "CONFLICT", ErrorCode.DUPLICATE_RESOURCE.getStatus());
            else if (ex.getMessage().contains("contacts_value_key"))
                throw new BaseException("Contact already exists.", "CONFLICT", ErrorCode.DUPLICATE_RESOURCE.getStatus());
        }
        return business;
    }

    public List<Business> getAllBusiness() {
        return businessDao.getBusinesses();
    }

    public void updateBusinessById(int id, Business business) {
        int row = businessDao.updateBusinessByid(id, business);
        if (row == 0)
            throw new BaseException("Business with id " + id + " not found.", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
    }

    public void deleteBusiness(int id) {
        int row = businessDao.deleteBusiness(id);
        if (row == 0)
            throw new BaseException("Business with id " + id + " not found.", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
    }
}
