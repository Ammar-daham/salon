package com.example.salon.dao;

import com.example.salon.model.Business;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BusinessDao {

    int insertBusiness(Business business);

    default int insertBusiness(String name, String description) {
        Business business = new Business(name, description);
        return insertBusiness(business);
    }

    List<Business> getBusinesses();

}
