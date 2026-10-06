package com.example.salon.dao;

import com.example.salon.model.SalonService;
import java.util.List;

public interface SalonServiceDao {
    Long addService(Long businessId, SalonService service);

    List<SalonService> getServicesForBusiness(Long businessId);

    List<SalonService> getServicesForStaff(long staffId);

    SalonService getServiceById(int businessId, int serviceId);

    int updateServiceById(long id, SalonService service);

    int deleteServiceById(long id);
}
