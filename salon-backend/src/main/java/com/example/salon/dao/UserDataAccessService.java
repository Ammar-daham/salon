package com.example.salon.dao;

import com.example.salon.model.Role;
import com.example.salon.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserDataAccessService implements UserDao
{
	private final JdbcTemplate jdbcTemplate;
	private final ContactDao contactDao;
	private final AddressDao addressDao;

	@Autowired
	public UserDataAccessService(JdbcTemplate jdbcTemplate, ContactDao contactDao, AddressDao addressDao)
	{
		this.jdbcTemplate = jdbcTemplate;
		this.contactDao = contactDao;
		this.addressDao = addressDao;
	}

	private RowMapper<User> userRowMapper()
	{
		return (rs, i) -> {
			User user = new User(
					rs.getLong("id"),
					rs.getString("first_name"),
					rs.getString("last_name"),
					Role.valueOf(rs.getString("role")),
					rs.getTimestamp("created_at").toInstant()
			);
			user.setEmail(rs.getString("email"));
			user.setPasswordHash(rs.getString("password_hash"));
			user.setBusinessId((Long) rs.getObject("business_id"));
			return user;
		};
	}

	@Override
	public Long addUser(User user)
	{
		String sql = """
				INSERT INTO users (first_name, last_name, role, email, password_hash, business_id)
				VALUES (?, ?, ?, ?, ?, ?)
				RETURNING id
				""";

		Long userId = jdbcTemplate.queryForObject(
				sql,
				Long.class,
				user.getFirstName(),
				user.getLastName(),
				user.getRole().name(),
				user.getEmail(),
				user.getPasswordHash(),
				user.getBusinessId()
		);
		user.setId(userId);

		// Insert addresses
		if (user.getAddresses() != null) {
			user.getAddresses().forEach(address ->
			{
				address.setUserId(userId);
				addressDao.addAddress(address);
			});
		}

		// Insert contacts
		if (user.getContacts() != null) {
			user.getContacts().forEach(contact ->
			{
				contact.setUserId(userId);
				contactDao.addContact(contact);
			});
		}
		return userId;
	}

	@Override
	public List<User> getAllUsers()
	{
		String sql = "SELECT * FROM users";
		List<User> users = jdbcTemplate.query(sql, userRowMapper());
		for (User user : users) {
			user.setAddresses(addressDao.getAddressesForUser(user.getId()));
			user.setContacts(contactDao.getContactsForUser(user.getId()));
		}
		return users;
	}

	@Override
	public List<User> getUsersByBusinessId(long businessId)
	{
		String sql = "SELECT * FROM users WHERE business_id = ?";
		List<User> users = jdbcTemplate.query(sql, userRowMapper(), businessId);
		for (User user : users) {
			user.setAddresses(addressDao.getAddressesForUser(user.getId()));
			user.setContacts(contactDao.getContactsForUser(user.getId()));
		}
		return users;
	}

	@Override
	public User getUserById(int id)
	{
		User user = jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", userRowMapper(), id);
		user.setAddresses(addressDao.getAddressesForUser(user.getId()));
		user.setContacts(contactDao.getContactsForUser(user.getId()));
		return user;
	}

	@Override
	public Optional<User> findByEmail(String email)
	{
		try {
			User user = jdbcTemplate.queryForObject("SELECT * FROM users WHERE email = ?", userRowMapper(), email);
			return Optional.ofNullable(user);
		} catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		}
	}

	@Override
	public long updateUserById(long id, User user)
	{
		String sql = "UPDATE users SET first_name = ?, last_name = ?, role = ? WHERE id = ?";
		long userId = (long) jdbcTemplate.update(sql, user.getFirstName(), user.getLastName(), user.getRole().name(), id);

		// Update contacts
		if (user.getContacts() != null) {
			user.getContacts().forEach(contact ->
			{
				contact.setUserId(id);
				contactDao.updateContactById(contact.getId(), contact);
			});
		}

		// Update addresses
		if (user.getAddresses() != null) {
			user.getAddresses().forEach(address ->
			{
				address.setUserId(id);
				addressDao.updateAddressById(address.getId(), address);
			});
		}
		return userId;
	}

	@Override
	public long deleteUserById(long id)
	{
		// BE-10: delete the user's own children from the canonical record, not from a client body.
		// Children first: the FKs are ON DELETE SET NULL, so deleting the user first would orphan
		// them (and a contact's globally-unique value would stay burned).
		contactDao.getContactsForUser(id).forEach(contact -> contactDao.deleteContactById(contact.getId()));
		addressDao.getAddressesForUser(id).forEach(address -> addressDao.deleteAddressById(address.getId()));

		return jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
	}
}
