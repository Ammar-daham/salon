package com.example.salon.service;

import com.example.salon.dao.CustomerDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Customer;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Customer records are PII, so every call is scoped to the caller's own salon. Staff (ADMIN and
 * EMPLOYEE) can list, view and add customers; only an ADMIN can edit or delete them.
 */
@Service
public class CustomerService
{
    private final CustomerDao customerDao;
    private final BusinessService businessService;

    @Autowired
    public CustomerService(CustomerDao customerDao, BusinessService businessService)
    {
        this.customerDao = customerDao;
        this.businessService = businessService;
    }

    @Transactional
    public Customer addCustomer(long businessId, Customer customer, AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        businessService.getBusinessById((int) businessId);

        customer.setBusinessId(businessId);
        customer.setId(customerDao.addCustomer(customer));
        return getCustomerById(businessId, customer.getId(), caller);
    }

    public List<Customer> getCustomersForBusiness(long businessId, AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        businessService.getBusinessById((int) businessId);
        return customerDao.getCustomersForBusiness(businessId);
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
    }

    @Transactional
    public void deleteCustomerById(long businessId, long customerId, AuthenticatedUser caller)
    {
        requireAdminOfBusiness(caller, businessId);
        int row = customerDao.deleteCustomerById(businessId, customerId);
        if (row == 0)
            throw new BaseException("Customer with id " + customerId + " not found.", ErrorCode.NOT_FOUND);
    }

    private static void requireAdminOfBusiness(AuthenticatedUser caller, long businessId)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        if (!AccessControl.isAdmin(caller))
            throw new AccessDeniedException("Only an admin can edit or delete customers");
    }
}
