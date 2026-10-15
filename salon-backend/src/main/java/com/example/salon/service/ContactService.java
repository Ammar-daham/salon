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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService
{
	private static final Logger log = LoggerFactory.getLogger(ContactService.class);

	private final ContactDao contactDao;
	private final UserDao userDao;

	@Autowired
	public ContactService(ContactDao contactDao, UserDao userDao)
	{
		this.contactDao = contactDao;
		this.userDao = userDao;
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

	public Contact getContactById(int id, AuthenticatedUser caller)
	{
		Contact c = fetch(id);
		// A business's contact is part of its public listing (customers can browse every salon);
		// a personal contact is only for those who may see its owner, and a 404 to anyone else.
		if (c.getBusinessId() == null && !AccessControl.canAccessUser(caller, c.getUserId(), userDao)) {
			throw notFound(id);
		}
		return c;
	}

	public void updateContactById(int id, Contact contact, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = contactDao.updateContactById(id, contact);
		if (row == 0)
			throw notFound(id);
		log.info("Updated contact {}", id);
	}

	public void delectContactById(int id, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = contactDao.deleteContactById(id);
		if (row == 0)
			throw notFound(id);
		log.info("Deleted contact {}", id);
	}

	private void requireWritePermission(int id, AuthenticatedUser caller)
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

	private Contact fetch(int id)
	{
		try {
			return contactDao.getContactById(id);
		} catch (EmptyResultDataAccessException e) {
			throw notFound(id);
		}
	}

	private static BaseException notFound(int id)
	{
		return new BaseException("Contact with id " + id + " not found", ErrorCode.NOT_FOUND);
	}
}
