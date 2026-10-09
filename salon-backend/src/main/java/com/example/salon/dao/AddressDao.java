package com.example.salon.dao;

import com.example.salon.model.Address;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AddressDao
{
	Long addAddress(Address address);

	List<Address> getAddressesForBusiness(Long id);

	List<Address> getAddressesForUser(Long id);

	/** Each business's addresses, in one query; a business with none has no entry. */
	Map<Long, List<Address>> getAddressesForBusinesses(Collection<Long> businessIds);

	/** Each user's addresses, in one query; a user with none has no entry. */
	Map<Long, List<Address>> getAddressesForUsers(Collection<Long> userIds);

	List<Address> getAllAddresses();

	Address getAddressById(int id);

	int updateAddressById(long id, Address address);

	int deleteAddressById(long id);
}
