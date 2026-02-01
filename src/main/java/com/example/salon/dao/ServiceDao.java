package com.example.salon.dao;

import com.example.salon.model.Service;

import java.util.List;

public interface ServiceDao {
    Long addService(Service service);

    List<Service> getAllServices();

    Service getServiceById(long id);

    int updateService(int id, Service service);

    int deleteService(int id);
}
