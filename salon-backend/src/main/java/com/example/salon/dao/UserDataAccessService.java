package com.example.salon.dao;

import com.example.salon.model.Address;
import com.example.salon.model.Contact;
import com.example.salon.model.Role;
import com.example.salon.model.User;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
			Timestamp updatedAt = rs.getTimestamp("updated_at");
			user.setUpdatedAt(updatedAt != null ? updatedAt.toInstant() : null);
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
	public Page<User> getUsers(Filter filter, Sort<SortBy> sort, PageQuery page)
	{
		ListQuery query = new ListQuery();
		if (filter.businessId() != null)
			query.where("business_id = ?", filter.businessId());
		if (filter.role() != null)
			query.where("role = ?", filter.role().name());
		String term = ListQuery.containing(filter.search());
		if (term != null)
			query.where("(concat_ws(' ', first_name, last_name) ILIKE ? OR email ILIKE ?)", term, term);
		List<String> columns = switch (sort.key()) {
			case NAME -> List.of("lower(first_name)", "lower(last_name)");
			case EMAIL -> List.of("lower(email)");
			case ROLE -> List.of("role");
			case CREATED_AT -> List.of("created_at");
		};

		Page<User> users = query.page(jdbcTemplate, "*", "users", ListQuery.orderBy(columns, sort.descending(), "id"),
				page, userRowMapper());
		loadChildren(users.items());
		return users;
	}

	@Override
	public User getUserById(int id)
	{
		User user = jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", userRowMapper(), id);
		loadChildren(List.of(user));
		return user;
	}

	/** Two queries however many users there are, rather than two per user (BE-15). */
	private void loadChildren(List<User> users)
	{
		List<Long> ids = users.stream().map(User::getId).toList();
		Map<Long, List<Address>> addresses = addressDao.getAddressesForUsers(ids);
		Map<Long, List<Contact>> contacts = contactDao.getContactsForUsers(ids);
		for (User user : users) {
			user.setAddresses(addresses.getOrDefault(user.getId(), new ArrayList<>()));
			user.setContacts(contacts.getOrDefault(user.getId(), new ArrayList<>()));
		}
	}

	@Override
	public Optional<User> findById(long id)
	{
		return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", userRowMapper(), id).stream().findFirst();
	}

	@Override
	public Optional<User> findByEmail(String email)
	{
		try {
			User user = jdbcTemplate.queryForObject("SELECT * FROM users WHERE lower(email) = lower(?)", userRowMapper(), email);
			return Optional.ofNullable(user);
		} catch (EmptyResultDataAccessException ex) {
			return Optional.empty();
		}
	}

	@Override
	public long updateUserById(long id, User user)
	{
		String sql = "UPDATE users SET first_name = ?, last_name = ?, role = ? WHERE id = ?";
		return (long) jdbcTemplate.update(sql, user.getFirstName(), user.getLastName(), user.getRole().name(), id);
	}

	@Override
	public long updatePasswordHash(long id, String passwordHash)
	{
		return jdbcTemplate.update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, id);
	}

	@Override
	public long deleteUserById(long id)
	{
		contactDao.getContactsForUser(id).forEach(contact -> contactDao.deleteContactById(contact.getId()));
		addressDao.getAddressesForUser(id).forEach(address -> addressDao.deleteAddressById(address.getId()));

		return jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
	}
}
