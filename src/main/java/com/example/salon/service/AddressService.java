package com.example.salon.service;

import com.example.salon.dao.AddressDao;
import com.example.salon.model.Address;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressDao addressDao;

    @Autowired
    public AddressService(AddressDao addressDao) {
        this.addressDao = addressDao;
    }

    public List<Address> getAllAddresses()
    {
        return addressDao.getAllAddresses();
    }
}
