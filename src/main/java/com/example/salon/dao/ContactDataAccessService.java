package com.example.salon.dao;

import com.example.salon.model.Contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ContactDataAccessService implements ContactDao
{
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
        String sql = """
                SELECT id, type, value, created_at
                FROM contacts WHERE business_id = ?;
                """;

        return jdbcTemplate.query(sql, (rs, i) ->
            new Contact(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("value"),
                rs.getTimestamp("created_at").toInstant()
            ), businessId
        );
    }

    @Override
    public List<Contact> getAllContacts() {
        String sql = "SELECT id, type, value, created_at FROM contacts";
        return jdbcTemplate.query(sql, (rs, i) ->
                new Contact(
                        rs.getLong("id"),
                        rs.getString("type"),
                        rs.getString("value"),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
    }

    @Override
    public Contact getContactById(int id)
    {
        String sql = "SELECT id, type, value, created_at FROM contacts WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, i) ->
            new Contact(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("value"),
                rs.getTimestamp("created_at").toInstant()
            ),
            id
        );
    }

    @Override
    public int updateContactById(int id, Contact contact)
    {
        String sql = "UPDATE contacts SET type = ?, value = ? WHERE id = ?";
        return jdbcTemplate.update(sql, contact.getType(), contact.getValue(), id);
    }

    @Override
    public int deleteContactById(int id) {
        String sql = "DELETE FROM contacts WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
