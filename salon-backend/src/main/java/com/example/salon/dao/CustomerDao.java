package com.example.salon.dao;

import com.example.salon.model.Customer;

import java.util.List;

public interface CustomerDao {
    Long addCustomer(Customer customer);

    List<Customer> getCustomersForBusiness(long businessId);

    Customer getCustomerById(long businessId, long customerId);

    int updateCustomerById(long businessId, long customerId, Customer customer);

    int deleteCustomerById(long businessId, long customerId);
}
