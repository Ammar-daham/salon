package com.example.salon.service;

import com.example.salon.dao.AddressDao;
import com.example.salon.dao.UserDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Address;
import com.example.salon.model.User;
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

	@Autowired
	public AddressService(AddressDao addressDao, UserDao userDao)
	{
		this.addressDao = addressDao;
		this.userDao = userDao;
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
		// a personal address is only for those who may see its owner.
		if (address.getBusinessId() == null) {
			requireUserAccess(caller, address.getUserId());
		}
		return address;
	}

	public void updateAddressById(int id, Address address, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = addressDao.updateAddressById(id, address);
		if (row == 0)
			throw new BaseException("Address with id " + id + " not found", ErrorCode.NOT_FOUND);
		log.info("Updated address {}", id);
	}

	public void deleteAddressById(int id, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = addressDao.deleteAddressById(id);
		if (row == 0)
			throw new BaseException("Address with id " + id + " not found", ErrorCode.NOT_FOUND);
		log.info("Deleted address {}", id);
	}

	private void requireWritePermission(int id, AuthenticatedUser caller)
	{
		Address address = fetch(id);
		// A business's address can only be changed by its admins; a personal address by those who may
		// change its owner.
		if (address.getBusinessId() != null) {
			AccessControl.requireAdminOfBusiness(caller, address.getBusinessId());
		} else {
			requireUserAccess(caller, address.getUserId());
		}
	}

	/** The owner, an admin of the owner's business, or a super admin: the same people as on /users. */
	private void requireUserAccess(AuthenticatedUser caller, long ownerId)
	{
		// An owner always exists: the foreign key deletes a user's addresses with them.
		User owner = userDao.findById(ownerId).orElseThrow();
		AccessControl.requireUserAccess(caller, owner);
	}

	private Address fetch(int id)
	{
		try {
			return addressDao.getAddressById(id);
		} catch (EmptyResultDataAccessException e) {
			throw new BaseException("Address with id " + id + " not found", ErrorCode.NOT_FOUND);
		}
	}
}
