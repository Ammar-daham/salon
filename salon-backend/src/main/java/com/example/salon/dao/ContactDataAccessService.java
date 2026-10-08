package com.example.salon.dao;

import com.example.salon.model.Contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ContactDataAccessService implements ContactDao
{
	private static final String SELECT = """
			SELECT id, type, value,
			created_at, updated_at,
			business_id, user_id
			FROM contacts
			""";

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public ContactDataAccessService(JdbcTemplate jdbcTemplate)
	{
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Long addContact(Contact contact)
	{
		String sql = """
				INSERT INTO contacts
				(type, value, business_id, user_id)
				VALUES (?, ?, ?, ?)
				RETURNING id;
				""";

		Long contactId = jdbcTemplate.queryForObject(
				sql,
				Long.class,
				contact.getType(),
				contact.getValue(),
				contact.getBusinessId(),
				contact.getUserId()
		);
		contact.setId(contactId);
		return contactId;
	}

	@Override
	public List<Contact> getContactsForBusiness(Long businessId)
	{
		return getContactsForBusinesses(List.of(businessId)).getOrDefault(businessId, List.of());
	}

	@Override
	public List<Contact> getContactsForUser(Long userId)
	{
		return getContactsForUsers(List.of(userId)).getOrDefault(userId, List.of());
	}

	@Override
	public Map<Long, List<Contact>> getContactsForBusinesses(Collection<Long> businessIds)
	{
		return byOwner(SELECT + "WHERE business_id = ANY(?) ORDER BY id", "business_id", businessIds);
	}

	@Override
	public Map<Long, List<Contact>> getContactsForUsers(Collection<Long> userIds)
	{
		return byOwner(SELECT + "WHERE user_id = ANY(?) ORDER BY id", "user_id", userIds);
	}

	/** One query for every owner's contacts (BE-15), grouped by the owner in ownerColumn. */
	private Map<Long, List<Contact>> byOwner(String sql, String ownerColumn, Collection<Long> ownerIds)
	{
		Map<Long, List<Contact>> byOwner = new HashMap<>();
		if (ownerIds.isEmpty()) {
			return byOwner;
		}
		jdbcTemplate.query(sql, rs -> {
			byOwner.computeIfAbsent(rs.getLong(ownerColumn), owner -> new ArrayList<>()).add(mapRow(rs));
		}, (Object) ownerIds.toArray(Long[]::new));
		return byOwner;
	}

	@Override
	public List<Contact> getAllContacts()
	{
		return jdbcTemplate.query(SELECT + "ORDER BY id", (rs, i) -> mapRow(rs));
	}

	@Override
	public Contact getContactById(int id)
	{
		return jdbcTemplate.queryForObject(SELECT + "WHERE id = ?", (rs, i) -> {
			Contact contact = mapRow(rs);
			// ContactService's owner-or-admin check needs the owner, so read it here.
			contact.setBusinessId(rs.getObject("business_id", Long.class));
			contact.setUserId(rs.getObject("user_id", Long.class));
			return contact;
		}, id);
	}

	@Override
	public int updateContactById(long id, Contact contact)
	{
		String sql = """
				UPDATE contacts SET type = ?, value = ?,
				updated_at = now()
				WHERE id = ?
				""";
		return jdbcTemplate.update(sql, contact.getType(), contact.getValue(), id);
	}

	@Override
	public int deleteContactById(long id)
	{
		String sql = "DELETE FROM contacts WHERE id = ?";
		return jdbcTemplate.update(sql, id);
	}

	private static Contact mapRow(ResultSet rs) throws SQLException
	{
		Timestamp updatedAt = rs.getTimestamp("updated_at");
		return new Contact(
				rs.getLong("id"),
				rs.getString("type"),
				rs.getString("value"),
				rs.getTimestamp("created_at").toInstant(),
				updatedAt != null ? updatedAt.toInstant() : null
		);
	}
}
