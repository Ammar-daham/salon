package com.example.salon.dao;

import com.example.salon.model.SalonService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SalonServiceDao {
    Long addService(Long businessId, SalonService service);

    List<SalonService> getServicesForBusiness(Long businessId);

    /** Each business's services, in one query; a business with none has no entry. */
    Map<Long, List<SalonService>> getServicesForBusinesses(Collection<Long> businessIds);

    List<SalonService> getServicesForStaff(long staffId);

    SalonService getServiceById(int businessId, int serviceId);

    int updateServiceById(long id, SalonService service);

    int deleteServiceById(long id);
}
