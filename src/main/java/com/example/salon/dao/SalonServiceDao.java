package com.example.salon.dao;

import com.example.salon.model.SalonService;
import java.util.List;

public interface SalonServiceDao {
    Long addService(SalonService service);

    List<SalonService> getServicesForBusiness(Long businessId);

    SalonService getServiceById(long id);

    int updateService(long id, SalonService service);

    int deleteService(long id);
}
