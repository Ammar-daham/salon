package com.example.salon.dao;

import com.example.salon.model.Business;

import java.util.List;

public interface BusinessDao {

    Long addBusiness(Business business);

    default Long addBusiness(String name, String description) {
        Business business = new Business(name, description);
        return addBusiness(business);
    }

    List<Business> getBusinesses();

}
