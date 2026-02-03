package com.example.salon.service;

import com.example.salon.dao.BusinessServiceDao;
import com.example.salon.dao.SalonServiceDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.SalonService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class BusinessSalonServiceService {

    private final SalonServiceDao salonServiceDao;
    private final BusinessServiceDao businessServiceDao;

    @Autowired
    public BusinessSalonServiceService(SalonServiceDao salonServiceDao, BusinessServiceDao businessServiceDao) {
        this.salonServiceDao = salonServiceDao;
        this.businessServiceDao = businessServiceDao;
    }

    @Transactional
    public SalonService createServiceForBusiness(Long businessId, SalonService salonService) {
        try {
            Long serviceId = salonServiceDao.addService(salonService);
            businessServiceDao.linkServiceToBusiness(businessId, serviceId);
            salonService.setId(serviceId);
        } catch (DuplicateKeyException ex) {

        }
        return salonService;
    }

    @Transactional
    public void updateServiceForBusiness(int id, SalonService salonService) {
        int row = salonServiceDao.updateService(id, salonService);
        if (row == 0)
            throw new BaseException("Service with id " + id + " not found.","NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
    }

    @Transactional
    public void deleteServiceForBusiness(int id) {
        int row = salonServiceDao.deleteService(id);
        if (row == 0)
            throw new BaseException("Service with id " + id + " not found.","NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
    }
}
