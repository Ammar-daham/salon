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
    public void addContactForBusiness(Long businessId, Contact contact)
    {
        String sql = """
                INSERT INTO contacts
                (type, value, business_id)
                VALUES (?, ?, ?)
                RETURNING id;
                """;

        Long contactId = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                contact.getType(),
                contact.getValue(),
                businessId
        );
        contact.setId(contactId);
    }

    @Override
    public void AddContactsForBusiness(Long businessId, List<Contact> contacts)
    {
        for (Contact contact : contacts) {
            addContactForBusiness(businessId, contact);
        }
    }
}
