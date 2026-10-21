package com.example.salon.service;

import com.example.salon.dao.AddressDao;
import com.example.salon.dao.UserDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Address;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService
{
	private static final Logger log = LoggerFactory.getLogger(AddressService.class);

	private final AddressDao addressDao;
	private final UserDao userDao;
	private final BusinessService businessService;

	@Autowired
	public AddressService(AddressDao addressDao, UserDao userDao, BusinessService businessService)
	{
		this.addressDao = addressDao;
		this.userDao = userDao;
		this.businessService = businessService;
	}

	/** A salon's new address, added by an admin of that salon or a super admin. */
	public Address addAddressForBusiness(long businessId, Address address, AuthenticatedUser caller)
	{
		AccessControl.requireAdminOfBusiness(caller, businessId);
		// A 404, rather than the error the fk_addresses_business foreign key would give.
		businessService.getBusinessById(businessId);
		address.setBusinessId(businessId);
		Long id = addressDao.addAddress(address);
		log.info("Created address {} for business {}", id, businessId);
		return fetch(id.intValue());
	}

	/** Every address for a super admin; for an admin, their own business's and its users', or none without one. */
	public List<Address> getAllAddresses(AuthenticatedUser caller)
	{
		if (AccessControl.isSuperAdmin(caller)) {
			return addressDao.getAllAddresses();
		}
		Long businessId = caller.getUser().getBusinessId();
		return businessId == null ? List.of() : addressDao.getAddressesForBusinessAndItsUsers(businessId);
	}

	public Address getAddressById(int id, AuthenticatedUser caller)
	{
		Address address = fetch(id);
		// A business's address is part of its public listing (customers can browse every salon);
		// a personal address is only for those who may see its owner, and a 404 to anyone else.
		if (address.getBusinessId() == null && !AccessControl.canAccessUser(caller, address.getUserId(), userDao)) {
			throw notFound(id);
		}
		return address;
	}

	public Address updateAddressById(int id, Address address, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = addressDao.updateAddressById(id, address);
		if (row == 0)
			throw notFound(id);
		log.info("Updated address {}", id);
		return fetch(id);
	}

	public void deleteAddressById(int id, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = addressDao.deleteAddressById(id);
		if (row == 0)
			throw notFound(id);
		log.info("Deleted address {}", id);
	}

	private void requireWritePermission(int id, AuthenticatedUser caller)
	{
		Address address = fetch(id);
		// A business's address can only be changed by its admins, and is public, so anyone else gets a 403.
		// A personal address only by those who may change its owner, and anyone else can't see it: a 404.
		if (address.getBusinessId() != null) {
			AccessControl.requireAdminOfBusiness(caller, address.getBusinessId());
		} else if (!AccessControl.canAccessUser(caller, address.getUserId(), userDao)) {
			throw notFound(id);
		}
	}

	private Address fetch(int id)
	{
		try {
			return addressDao.getAddressById(id);
		} catch (EmptyResultDataAccessException e) {
			throw notFound(id);
		}
	}

	private static BaseException notFound(int id)
	{
		return new BaseException("Address with id " + id + " not found", ErrorCode.NOT_FOUND);
	}
}
