package com.example.salon.dao;

import com.example.salon.model.Address;

import java.util.List;

public interface AddressDao {

    void addAddressForBusiness(Long businessId, Address address);

    void AddAddressesForBusiness(Long businessId, List<Address> addresses);

    List<Address> getAddressesForBusiness(Long businessId);
}
