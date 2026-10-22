package com.example.salon.service;

import com.example.salon.dao.BusinessDao;
import com.example.salon.dao.Constraints;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Business;
import com.example.salon.model.Status;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;
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


@Service
public class BusinessService
{
    private static final Logger log = LoggerFactory.getLogger(BusinessService.class);

    /** V1's inline UNIQUE on businesses.name, under the name Postgres gave it. */
    private static final String NAME_UNIQUE = "businesses_name_key";

    private final BusinessDao businessDao;

    @Autowired
    public BusinessService(BusinessDao businessDao) 
    {
        this.businessDao = businessDao;
    }

    @Transactional
    public Business addBusiness(Business business) 
    {
        Long id;
        try {
            id = businessDao.addBusiness(business);
        } catch (DuplicateKeyException ex) {
            if (Constraints.violated(ex, NAME_UNIQUE))
                throw nameTaken(business);
            if (Constraints.violated(ex, "contacts_business_value_unique_idx"))
                throw new BaseException("Contact already exists.", ErrorCode.DUPLICATE_RESOURCE);
            log.warn("Unmapped duplicate-key creating business '{}'", business.getName(), ex);
            throw new BaseException("Business could not be created due to a conflict.", ErrorCode.DUPLICATE_RESOURCE);
        }
        log.info("Created business {} with status {}", id, business.getStatus());
        return getBusinessById(id);
    }

    /** Super admins see every salon; anyone else the approved ones and their own, counted in SQL so pages add up. */
    public Page<Business> getBusinesses(String search, Status status, Sort<BusinessDao.SortBy> sort, PageQuery page,
            AuthenticatedUser caller)
    {
        BusinessDao.Filter filter = new BusinessDao.Filter(search, status, AccessControl.isSuperAdmin(caller),
                caller.getUser().getBusinessId());
        return businessDao.getBusinesses(filter, sort, page);
    }

    public Business getBusinessById(long id, AuthenticatedUser caller) 
    {
        Business business = getBusinessById(id);
        // 404 rather than 403, so no one but a super admin can probe which unapproved salons exist.
        if (!isVisibleTo(business, caller))
            throw new BaseException("Business with id " + id + " not found", ErrorCode.NOT_FOUND);
        return business;
    }

    public Business getBusinessById(long id) 
    {
        Business business;
        try {
            business = businessDao.getBusinessById(id);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Business with id " + id + " not found", ErrorCode.NOT_FOUND);
        }
        return business;
    }

    /** Another salon's ADMIN doesn't see one that isn't approved: its details are the owner's until it is. */
    private static boolean isVisibleTo(Business business, AuthenticatedUser caller)
    {
        if (AccessControl.isSuperAdmin(caller) || business.getStatus() == Status.APPROVED)
            return true;
        Long callerBusinessId = caller.getUser().getBusinessId();
        return callerBusinessId != null && callerBusinessId.equals(business.getId());
    }

    @Transactional
    public Business updateBusinessById(long id, Business business, AuthenticatedUser caller) 
    {
        getBusinessById(id);

        // ADMIN may only edit their own salon; SUPER_ADMIN may edit any.
        AccessControl.requireBusinessAccess(caller, id);

        int row;
        try {
            row = businessDao.updateBusinessById(id, business);
        } catch (DuplicateKeyException ex) {
            if (!Constraints.violated(ex, NAME_UNIQUE))
                throw ex;
            throw nameTaken(business);
        }
        if (row == 0)
            throw new BaseException("Business with id " + id + " not found.", ErrorCode.NOT_FOUND);
        log.info("Updated business {}", id);
        return getBusinessById(id);
    }

    /**
     * Approving, rejecting and suspending a salon is platform moderation, reserved for SUPER_ADMIN, and kept
     * apart from its details so a settings form can't undo it. Asking for the status it already has changes
     * nothing.
     */
    @Transactional
    public Business changeStatus(long id, Status status, AuthenticatedUser caller)
    {
        if (!AccessControl.isSuperAdmin(caller))
            throw new AccessDeniedException("Only a super admin can change a business's status");
        Business existing = getBusinessById(id);
        if (status != existing.getStatus()) {
            businessDao.updateBusinessStatus(id, status);
            log.info("Business {} status changed from {} to {}", id, existing.getStatus(), status);
        }
        return getBusinessById(id);
    }

    @Transactional
    public void deleteBusiness(long id, AuthenticatedUser caller)
    {
        getBusinessById(id);

        // ADMIN can only delete their own salon, never another tenant's.
        AccessControl.requireBusinessAccess(caller, id);

        int row;
        try {
            row = businessDao.deleteBusiness(id);
        } catch (DataIntegrityViolationException ex) {
            if (!Constraints.violated(ex, "fk_users_business"))
                throw ex;
            // fk_users_business is ON DELETE RESTRICT; the transaction rolls back the child deletes.
            throw new BaseException("Business with id " + id + " still has staff. Remove or move them first.",
                    ErrorCode.DUPLICATE_RESOURCE);
        }
        if (row == 0)
            throw new BaseException("Business with id " + id + " not found.", ErrorCode.NOT_FOUND);
        log.info("Deleted business {} with its services, addresses, contacts and customers", id);
    }

    private static BaseException nameTaken(Business business)
    {
        return new BaseException("Business with name " + business.getName() + " already exists.",
                ErrorCode.DUPLICATE_RESOURCE);
    }
}
