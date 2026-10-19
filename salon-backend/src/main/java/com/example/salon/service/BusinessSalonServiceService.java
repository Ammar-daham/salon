package com.example.salon.service;

import com.example.salon.dao.AppointmentDao;
import com.example.salon.dao.SalonServiceDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.SalonService;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessSalonServiceService 
{
    private static final Logger log = LoggerFactory.getLogger(BusinessSalonServiceService.class);

    private final SalonServiceDao salonServiceDao;
    private final AppointmentDao appointmentDao;
    private final BusinessService businessService;

    @Autowired
    public BusinessSalonServiceService(SalonServiceDao salonServiceDao, AppointmentDao appointmentDao,
            BusinessService businessService)
    {
        this.salonServiceDao = salonServiceDao;
        this.appointmentDao = appointmentDao;
        this.businessService = businessService;
    }

    @Transactional
    public SalonService createServiceForBusiness(Long businessId, SalonService salonService, AuthenticatedUser caller)
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        // A 404, rather than the 400 the fk_services_business foreign key would give.
        businessService.getBusinessById(businessId.intValue());
        Long serviceId;
        try {
            serviceId = salonServiceDao.addService(businessId, salonService);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("Service could not be created due to a conflict.", ErrorCode.DUPLICATE_RESOURCE);
        }
        log.info("Created service {} in business {}", serviceId, businessId);
        return getServiceById(businessId.intValue(), serviceId.intValue());
    }

    /** One of a salon's services: public like the salon itself, so a 404 for a salon you can't see. */
    public SalonService getServiceById(int businessId, int serviceId, AuthenticatedUser caller)
    {
        businessService.getBusinessById(businessId, caller);
        return getServiceById(businessId, serviceId);
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
    public SalonService updateServiceForBusiness(int businessId, int serviceId, SalonService salonService, AuthenticatedUser caller) 
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        getServiceById(businessId, serviceId);
        int row = salonServiceDao.updateServiceById(serviceId, salonService);
        if (row == 0)
            throw new BaseException("Service with id " + serviceId + " not found.", ErrorCode.NOT_FOUND);
        log.info("Updated service {} in business {}", serviceId, businessId);
        return getServiceById(businessId, serviceId);
    }

    @Transactional
    public void deleteServiceForBusiness(int businessId, int serviceId, AuthenticatedUser caller) 
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        getServiceById(businessId, serviceId);
        if (appointmentDao.hasUpcomingAppointmentsForService(serviceId))
            throw new BaseException("Service with id " + serviceId + " still has upcoming appointments. "
                    + "Cancel or move them first.", ErrorCode.DUPLICATE_RESOURCE);
        int row = salonServiceDao.deleteServiceById(serviceId);
        if (row == 0)
            throw new BaseException("Service with id " + serviceId + " not found.", ErrorCode.NOT_FOUND);
        log.info("Deleted service {} from business {}", serviceId, businessId);
    }
}
