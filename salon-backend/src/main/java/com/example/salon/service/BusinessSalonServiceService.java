package com.example.salon.service;

import com.example.salon.dao.BusinessServiceDao;
import com.example.salon.dao.SalonServiceDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.SalonService;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Service
public class BusinessSalonServiceService 
{
    private final SalonServiceDao salonServiceDao;
    private final BusinessServiceDao businessServiceDao;

    @Autowired
    public BusinessSalonServiceService(SalonServiceDao salonServiceDao, BusinessServiceDao businessServiceDao) 
    {
        this.salonServiceDao = salonServiceDao;
        this.businessServiceDao = businessServiceDao;
    }

    @Transactional
    public SalonService createServiceForBusiness(Long businessId, SalonService salonService, AuthenticatedUser caller) 
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        try {
            Long serviceId = salonServiceDao.addService(salonService);
            businessServiceDao.linkServiceToBusiness(businessId, serviceId);
            salonService.setId(serviceId);
        } catch (DuplicateKeyException ex) {
            // BE-09: an empty catch returned a service with no id as if it had been created. Surface
            // the failure as a conflict instead.
            throw new BaseException("Service could not be created due to a conflict.", ErrorCode.DUPLICATE_RESOURCE);
        }
        return salonService;
    }

    public SalonService getServiceById(int businessId, int serviceId) 
    {
        SalonService ss;
        try {
            ss = salonServiceDao.getServiceById(businessId, serviceId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Service with id " + serviceId + " not found.", ErrorCode.NOT_FOUND);
        }
        return ss;
    }

    @Transactional
    public void updateServiceForBusiness(int businessId, int serviceId, SalonService salonService, AuthenticatedUser caller) 
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        //  Confirm the service really belongs to this business
        // (getServiceById joins business_service and 404s otherwise) before mutating it.
        getServiceById(businessId, serviceId);
        int row = salonServiceDao.updateServiceById(serviceId, salonService);
        if (row == 0)
            throw new BaseException("Service with id " + serviceId + " not found.", ErrorCode.NOT_FOUND);
    }

    @Transactional
    public void deleteServiceForBusiness(int businessId, int serviceId, AuthenticatedUser caller) 
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        getServiceById(businessId, serviceId);
        int row = salonServiceDao.deleteServiceById(serviceId);
        if (row == 0)
            throw new BaseException("Service with id " + serviceId + " not found.", ErrorCode.NOT_FOUND);
    }
}
