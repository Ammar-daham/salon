package com.example.salon.service;

import com.example.salon.dao.ContactDao;
import com.example.salon.dao.UserDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Contact;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService
{
	private static final Logger log = LoggerFactory.getLogger(ContactService.class);

	/** V5's unique indexes on (business_id, value) and (user_id, value): one owner can't list a value twice. */
	private static final List<String> VALUE_UNIQUE = List.of(
			"contacts_business_value_unique_idx", "contacts_user_value_unique_idx");

	private final ContactDao contactDao;
	private final UserDao userDao;
	private final BusinessService businessService;

	@Autowired
	public ContactService(ContactDao contactDao, UserDao userDao, BusinessService businessService)
	{
		this.contactDao = contactDao;
		this.userDao = userDao;
		this.businessService = businessService;
	}

	/** A salon's new contact, added by an admin of that salon or a super admin. */
	public Contact addContactForBusiness(long businessId, Contact contact, AuthenticatedUser caller)
	{
		AccessControl.requireAdminOfBusiness(caller, businessId);
		// A 404, rather than the error the fk_contacts_business foreign key would give.
		businessService.getBusinessById(businessId);
		contact.setBusinessId(businessId);
		Long id;
		try {
			id = contactDao.addContact(contact);
		} catch (DuplicateKeyException ex) {
			throw duplicateValue(ex);
		}
		log.info("Created contact {} for business {}", id, businessId);
		return fetch(id);
	}

	/** Every contact for a super admin; for an admin, their own business's and its users', or none without one. */
	public List<Contact> getAllContacts(AuthenticatedUser caller)
	{
		if (AccessControl.isSuperAdmin(caller)) {
			return contactDao.getAllContacts();
		}
		Long businessId = caller.getUser().getBusinessId();
		return businessId == null ? List.of() : contactDao.getContactsForBusinessAndItsUsers(businessId);
	}

	public Contact getContactById(long id, AuthenticatedUser caller)
	{
		Contact c = fetch(id);
		// A business's contact is part of its public listing (customers can browse every salon);
		// a personal contact is only for those who may see its owner, and a 404 to anyone else.
		if (c.getBusinessId() == null && !AccessControl.canAccessUser(caller, c.getUserId(), userDao)) {
			throw notFound(id);
		}
		return c;
	}

	public Contact updateContactById(long id, Contact contact, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row;
		try {
			row = contactDao.updateContactById(id, contact);
		} catch (DuplicateKeyException ex) {
			throw duplicateValue(ex);
		}
		if (row == 0)
			throw notFound(id);
		log.info("Updated contact {}", id);
		return fetch(id);
	}

	public void deleteContactById(long id, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = contactDao.deleteContactById(id);
		if (row == 0)
			throw notFound(id);
		log.info("Deleted contact {}", id);
	}

	private void requireWritePermission(long id, AuthenticatedUser caller)
	{
		Contact c = fetch(id);
		// A business's contact can only be changed by its admins, and is public, so anyone else gets a 403.
		// A personal contact only by those who may change its owner, and anyone else can't see it: a 404.
		if (c.getBusinessId() != null) {
			AccessControl.requireAdminOfBusiness(caller, c.getBusinessId());
		} else if (!AccessControl.canAccessUser(caller, c.getUserId(), userDao)) {
			throw notFound(id);
		}
	}

	/** A 409 when the owner already has the value, or the exception itself for any other clash. */
	private static RuntimeException duplicateValue(DuplicateKeyException ex)
	{
		String reason = ex.getMessage();
		if (reason == null || VALUE_UNIQUE.stream().noneMatch(reason::contains))
			return ex;
		return new BaseException("Contact already exists.", ErrorCode.DUPLICATE_RESOURCE);
	}

	private Contact fetch(long id)
	{
		try {
			return contactDao.getContactById(id);
		} catch (EmptyResultDataAccessException e) {
			throw notFound(id);
		}
	}

	private static BaseException notFound(long id)
	{
		return new BaseException("Contact with id " + id + " not found", ErrorCode.NOT_FOUND);
	}
}
