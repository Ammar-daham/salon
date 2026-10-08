package com.example.salon.dao;

import com.example.salon.model.Contact;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ContactDao
{
	Long addContact(Contact contact);

	List<Contact> getContactsForBusiness(Long businessId);

	List<Contact> getContactsForUser(Long userId);

	/** Each business's contacts, in one query; a business with none has no entry. */
	Map<Long, List<Contact>> getContactsForBusinesses(Collection<Long> businessIds);

	/** Each user's contacts, in one query; a user with none has no entry. */
	Map<Long, List<Contact>> getContactsForUsers(Collection<Long> userIds);

	List<Contact> getAllContacts();

	Contact getContactById(int id);

	int updateContactById(long id, Contact contact);

	int deleteContactById(long id);
}
