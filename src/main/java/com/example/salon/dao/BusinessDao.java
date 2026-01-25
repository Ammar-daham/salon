package com.example.salon.dao;

import com.example.salon.model.Business;

import java.util.List;

public interface BusinessDao {
    Long addBusiness(Business business);

    List<Business> getBusinesses();

    int deleteBusiness(int id);
}
