package com.example.salon.dao;

import com.example.salon.model.Address;

import java.util.List;

public interface AddressDao {

    Long addAddress(Address address);

    List<Address> getAddressesForBusiness(Long businessId);

    List<Address> getAllAddresses();

    Address getAddressById(int id);

    int updateAddressById(int id, Address address);

    int deleteAddressById(int id);
}
