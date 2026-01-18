package com.example.salon.service;

import com.example.salon.dao.AddressDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Address;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
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

    public Address getAddressById(int id)
    {
        Address address;
        try {
           address = addressDao.getAddressById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new BaseException("Address with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
        }
        return address;
    }
}
