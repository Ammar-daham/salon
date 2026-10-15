package com.example.salon.service;

import com.example.salon.dao.ContactDao;
import com.example.salon.dao.UserDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Contact;
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
		// a personal contact is only for those who may see its owner.
		if (c.getBusinessId() == null) {
			requireUserAccess(caller, c.getUserId());
		}
		return c;
	}

	public void updateContactById(int id, Contact contact, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = contactDao.updateContactById(id, contact);
		if (row == 0)
			throw new BaseException("Contact with id " + id + " not found", ErrorCode.NOT_FOUND);
		log.info("Updated contact {}", id);
	}

	public void delectContactById(int id, AuthenticatedUser caller)
	{
		requireWritePermission(id, caller);
		int row = contactDao.deleteContactById(id);
		if (row == 0)
			throw new BaseException("Contact with id " + id + " not found", ErrorCode.NOT_FOUND);
		log.info("Deleted contact {}", id);
	}

	private void requireWritePermission(int id, AuthenticatedUser caller)
	{
		Contact c = fetch(id);
		// A business's contact can only be changed by its admins; a personal contact by those who may
		// change its owner.
		if (c.getBusinessId() != null) {
			AccessControl.requireAdminOfBusiness(caller, c.getBusinessId());
		} else {
			requireUserAccess(caller, c.getUserId());
		}
	}

	/** The owner, an admin of the owner's business, or a super admin: the same people as on /users. */
	private void requireUserAccess(AuthenticatedUser caller, long ownerId)
	{
		// An owner always exists: the foreign key deletes a user's contacts with them.
		User owner = userDao.findById(ownerId).orElseThrow();
		AccessControl.requireUserAccess(caller, owner);
	}

	private Contact fetch(int id)
	{
		try {
			return contactDao.getContactById(id);
		} catch (EmptyResultDataAccessException e) {
			throw new BaseException("Contact with id " + id + " not found", ErrorCode.NOT_FOUND);
		}
	}
}
