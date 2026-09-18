package com.example.salon.service;

import com.example.salon.dao.AddressDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Address;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService
{

	private final AddressDao addressDao;

	@Autowired
	public AddressService(AddressDao addressDao)
	{
		this.addressDao = addressDao;
	}

	public List<Address> getAllAddresses()
	{
		return addressDao.getAllAddresses();
	}

	public Address getAddressById(int id, AuthenticatedUser caller)
	{
		Address address = fetch(id);
		// A business's address is part of its public listing (customers can browse every salon);
		// a personal address is only visible to its owner or an admin.
		if (address.getBusinessId() == null) {
			AccessControl.requireSelfOrAdmin(caller, address.getUserId());
		}
		return address;
	}

	public void updateAddressById(int id, Address address, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = addressDao.updateAddressById(id, address);
		if (row == 0)
			throw new BaseException("Address with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
	}

	public void deleteAddressById(int id, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = addressDao.deleteAddressById(id);
		if (row == 0)
			throw new BaseException("Address with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
	}

	private void requireWritePermission(int id, AuthenticatedUser caller)
	{
		Address address = fetch(id);
		// A business's address can only be changed by an admin; a personal address by its owner or an admin.
		if (address.getBusinessId() != null) {
			AccessControl.requireAdmin(caller);
		} else {
			AccessControl.requireSelfOrAdmin(caller, address.getUserId());
		}
	}

	private Address fetch(int id)
	{
		try {
			return addressDao.getAddressById(id);
		} catch (EmptyResultDataAccessException e) {
			throw new BaseException("Address with id " + id + " not found", "NOT_FOUND", ErrorCode.NOT_FOUND.getStatus());
		}
	}
}
