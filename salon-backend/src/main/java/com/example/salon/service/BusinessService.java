package com.example.salon.service;

import com.example.salon.dao.BusinessDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Business;
import com.example.salon.model.Status;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusinessService
{
    private static final Logger log = LoggerFactory.getLogger(BusinessService.class);

    private final BusinessDao businessDao;

    @Autowired
    public BusinessService(BusinessDao businessDao) 
    {
        this.businessDao = businessDao;
    }

    @Transactional
    public Business addBusiness(Business business) 
    {
        try {
            businessDao.addBusiness(business);
        } catch (DuplicateKeyException ex) {
            String reason = ex.getMessage();
            if (reason != null && reason.contains("businesses_name_unique"))
                throw new BaseException("Business with name " + business.getName() + " already exists.", ErrorCode.DUPLICATE_RESOURCE);
            if (reason != null && reason.contains("contacts_business_value_unique_idx"))
                throw new BaseException("Contact already exists.", ErrorCode.DUPLICATE_RESOURCE);
            log.warn("Unmapped duplicate-key creating business '{}'", business.getName(), ex);
            throw new BaseException("Business could not be created due to a conflict.", ErrorCode.DUPLICATE_RESOURCE);
        }
        log.info("Created business {} with status {}", business.getId(), business.getStatus());
        return business;
    }

    public List<Business> getAllBusiness(AuthenticatedUser caller) 
    {
        return businessDao.getBusinesses().stream()
                .filter(business -> isVisibleTo(business, caller))
                .toList();
    }

    public Business getBusinessById(int id, AuthenticatedUser caller) 
    {
        Business business = getBusinessById(id);
        // 404 rather than 403, so a non-admin can't probe which unapproved salons exist.
        if (!isVisibleTo(business, caller))
            throw new BaseException("Business with id " + id + " not found", ErrorCode.NOT_FOUND);
        return business;
    }

    public Business getBusinessById(int id) 
    {
        Business business;
        try {
            business = businessDao.getBusinessById(id);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Business with id " + id + " not found", ErrorCode.NOT_FOUND);
        }
        return business;
    }

    private static boolean isVisibleTo(Business business, AuthenticatedUser caller)
    {
        if (AccessControl.isAdmin(caller) || business.getStatus() == Status.APPROVED)
            return true;
        Long callerBusinessId = caller.getUser().getBusinessId();
        return callerBusinessId != null && callerBusinessId.equals(business.getId());
    }

    @Transactional
    public void updateBusinessById(int id, Business business, AuthenticatedUser caller) 
    {
        Business existing = getBusinessById(id);

        // ADMIN may only edit their own salon; SUPER_ADMIN may edit any.
        AccessControl.requireBusinessAccess(caller, id);
        // Approving/rejecting/suspending a salon is platform moderation, reserved for SUPER_ADMIN.
        if (!AccessControl.isSuperAdmin(caller)
                && business.getStatus() != null && business.getStatus() != existing.getStatus()) {
            throw new AccessDeniedException("Only a super admin can change a business's status");
        }

        int row = businessDao.updateBusinessById(id, business);
        if (row == 0)
            throw new BaseException("Business with id " + id + " not found.", ErrorCode.NOT_FOUND);
        if (business.getStatus() != null && business.getStatus() != existing.getStatus())
            log.info("Business {} status changed from {} to {}", id, existing.getStatus(), business.getStatus());
        log.info("Updated business {}", id);
    }

    @Transactional
    public void deleteBusiness(int id, AuthenticatedUser caller)
    {
        getBusinessById(id);

        // ADMIN can only delete their own salon, never another tenant's.
        AccessControl.requireBusinessAccess(caller, id);

        int row;
        try {
            row = businessDao.deleteBusiness(id);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() == null || !ex.getMessage().contains("fk_users_business"))
                throw ex;
            // fk_users_business is ON DELETE RESTRICT; the transaction rolls back the child deletes.
            throw new BaseException("Business with id " + id + " still has staff. Remove or move them first.",
                    ErrorCode.DUPLICATE_RESOURCE);
        }
        if (row == 0)
            throw new BaseException("Business with id " + id + " not found.", ErrorCode.NOT_FOUND);
        log.info("Deleted business {} with its services, addresses, contacts and customers", id);
    }
}
