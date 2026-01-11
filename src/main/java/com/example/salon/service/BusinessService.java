package com.example.salon.service;

import com.example.salon.dao.BusinessDao;
import com.example.salon.model.Business;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessService {

    private final BusinessDao businessDao;

    @Autowired
    public BusinessService(@Qualifier("postgres") BusinessDao businessDao)
    {
        this.businessDao = businessDao;
    }

    @Transactional
    public void addBusiness(Business business) {
        businessDao.addBusiness(business);
    }

    public List<Business> getAllBusiness() {
        return businessDao.getBusinesses();
    }

}
