package com.example.salon.service;

import com.example.salon.dao.BusinessDao;
import com.example.salon.model.Business;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessService {

    private final BusinessDao businessDao;

    @Autowired
    public BusinessService(@Qualifier("postgres") BusinessDao businessDao) {
        this.businessDao = businessDao;
    }

    public int addBusiness(Business business) {
        return businessDao.insertBusiness(business);
    }

    public List<Business> getAllBusiness() {
        return businessDao.getBusinesses();
    }

}
