package com.example.salon.dao;

import com.example.salon.model.OpeningInterval;

import java.util.List;

public interface BusinessHoursDao {
    List<OpeningInterval> getHoursForBusiness(long businessId);

    void replaceHoursForBusiness(long businessId, List<OpeningInterval> hours);
}
