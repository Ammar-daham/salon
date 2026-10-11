package com.example.salon.service;

import com.example.salon.dao.AppointmentDao;
import com.example.salon.dao.CustomerDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Customer;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Customer records are PII, so every call is scoped to the caller's own salon. Staff (ADMIN and
 * EMPLOYEE) can list, view and add customers; only an ADMIN can edit or delete them.
 */
@Service
public class CustomerService
{
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerDao customerDao;
    private final AppointmentDao appointmentDao;
    private final BusinessService businessService;

    @Autowired
    public CustomerService(CustomerDao customerDao, AppointmentDao appointmentDao, BusinessService businessService)
    {
        this.customerDao = customerDao;
        this.appointmentDao = appointmentDao;
        this.businessService = businessService;
    }

    @Transactional
    public Customer addCustomer(long businessId, Customer customer, AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        businessService.getBusinessById((int) businessId);

        customer.setBusinessId(businessId);
        customer.setId(customerDao.addCustomer(customer));
        log.info("Created customer {} in business {}", customer.getId(), businessId);
        return getCustomerById(businessId, customer.getId(), caller);
    }

    public Page<Customer> getCustomersForBusiness(long businessId, String search, Sort<CustomerDao.SortBy> sort,
            PageQuery page, AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        businessService.getBusinessById((int) businessId);
        return customerDao.getCustomers(new CustomerDao.Filter(businessId, search), sort, page);
    }

    /**
     * Customers across salons (BE-15): every salon's for a super admin, or one salon's with businessId.
     * Anyone else only ever gets their own salon's, whether they name it or not.
     */
    public Page<Customer> getCustomers(Long businessId, String search, Sort<CustomerDao.SortBy> sort, PageQuery page,
            AuthenticatedUser caller)
    {
        Long scope = businessId;
        if (!AccessControl.isSuperAdmin(caller)) {
            scope = businessId != null ? businessId : caller.getUser().getBusinessId();
            if (scope == null)
                throw new AccessDeniedException("You can only access your own business");
            AccessControl.requireStaffOfBusiness(caller, scope);
        }
        return customerDao.getCustomers(new CustomerDao.Filter(scope, search), sort, page);
    }

    /** A customer by id alone. Another salon's is a 404, so ids can't be used to find out who's whose client. */
    public Customer getCustomer(long customerId, AuthenticatedUser caller)
    {
        return customerDao.findCustomer(customerId)
                .filter(customer -> AccessControl.isStaffOfBusiness(caller, customer.getBusinessId()))
                .orElseThrow(() -> new BaseException("Customer with id " + customerId + " not found.",
                        ErrorCode.NOT_FOUND));
    }

    public Customer getCustomerById(long businessId, long customerId, AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        try {
            return customerDao.getCustomerById(businessId, customerId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Customer with id " + customerId + " not found.", ErrorCode.NOT_FOUND);
        }
    }

    @Transactional
    public void updateCustomerById(long businessId, long customerId, Customer customer, AuthenticatedUser caller)
    {
        requireAdminOfBusiness(caller, businessId);
        int row = customerDao.updateCustomerById(businessId, customerId, customer);
        if (row == 0)
            throw new BaseException("Customer with id " + customerId + " not found.", ErrorCode.NOT_FOUND);
        log.info("Updated customer {} in business {}", customerId, businessId);
    }

    @Transactional
    public void deleteCustomerById(long businessId, long customerId, AuthenticatedUser caller)
    {
        requireAdminOfBusiness(caller, businessId);
        getCustomerById(businessId, customerId, caller);
        if (appointmentDao.hasUpcomingAppointmentsForCustomer(customerId))
            throw new BaseException("Customer with id " + customerId + " still has upcoming appointments. "
                    + "Cancel or move them first.", ErrorCode.DUPLICATE_RESOURCE);
        int row = customerDao.deleteCustomerById(businessId, customerId);
        if (row == 0)
            throw new BaseException("Customer with id " + customerId + " not found.", ErrorCode.NOT_FOUND);
        log.info("Deleted customer {} from business {}", customerId, businessId);
    }

    private static void requireAdminOfBusiness(AuthenticatedUser caller, long businessId)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        if (!AccessControl.isAdmin(caller))
            throw new AccessDeniedException("Only an admin can edit or delete customers");
    }
}
