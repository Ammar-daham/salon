package com.example.salon.dao;

import com.example.salon.model.Customer;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;

import java.util.Optional;

public interface CustomerDao {
    enum SortBy { NAME, CREATED_AT, BUSINESS_NAME }

    /** businessId narrows to one salon, null for every salon; search matches the name, email or phone. */
    record Filter(Long businessId, String search) {}

    Long addCustomer(Customer customer);

    Page<Customer> getCustomers(Filter filter, Sort<SortBy> sort, PageQuery page);

    Customer getCustomerById(long businessId, long customerId);

    /** A current customer of any salon. */
    Optional<Customer> findCustomer(long customerId);

    int updateCustomerById(long businessId, long customerId, Customer customer);

    int deleteCustomerById(long businessId, long customerId);
}
