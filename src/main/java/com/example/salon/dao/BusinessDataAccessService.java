package com.example.salon.dao;

import com.example.salon.model.Business;
import com.example.salon.model.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class BusinessDataAccessService implements BusinessDao {
    private final JdbcTemplate jdbcTemplate;
    private final AddressDao addressDao;
    private final ContactDao contactDao;

    @Autowired
    public BusinessDataAccessService(JdbcTemplate jdbcTemplate, AddressDao addressDao, ContactDao contactDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.addressDao = addressDao;
        this.contactDao = contactDao;
    }

    @Override
    @Transactional
    public Long addBusiness(Business business) {
        String sql = """
                INSERT INTO businesses
                (name, description, image)
                VALUES (?, ?, ?)
                RETURNING id
                """;

        Long businessId = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                business.getName(),
                business.getDescription(),
                business.getImage()
        );
        business.setId(businessId);

        // Insert addresses if present
        if (business.getAddresses() != null) {
            business.getAddresses().forEach(address ->
            {
                address.setBusinessId(businessId);
                addressDao.addAddress(address);
            });
        }

        // Insert contacts if present
        if (business.getContacts() != null) {
            business.getContacts().forEach(contact ->
            {
                contact.setBusinessId(businessId);
                contactDao.addContact(contact);
            });
        }
        return businessId;
    }

    public List<Business> getBusinesses() {
        String sql = """
                SELECT id, name, description,
                updated_at, created_at, image
                FROM businesses
                """;
        List<Business> businesses = jdbcTemplate.query(sql, (rs, i) -> {
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            return new Business(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getTimestamp("created_at").toInstant(),
                    updatedAt != null ? updatedAt.toInstant() : null,
                    rs.getString("image")
            );
        });

        for (Business business : businesses) {
            business.setAddresses(addressDao.getAddressesForBusiness(business.getId()));
            business.setContacts(contactDao.getContactsForBusiness(business.getId()));
        }

        return businesses;
    }

    @Override
    public Business getBusinessById(int id) {
        String sql = """
                SELECT id, name, description,
                updated_at, created_at, image
                FROM businesses
                WHERE id = ?
                """;
        Business business = jdbcTemplate.queryForObject(sql, (rs, i) -> {
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    return new Business(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getTimestamp("created_at").toInstant(),
                            updatedAt != null ? updatedAt.toInstant() : null,
                            rs.getString("image")
                    );
                }, id
        );

        business.setAddresses(addressDao.getAddressesForBusiness(business.getId()));
        business.setContacts(contactDao.getContactsForBusiness(business.getId()));

        return business;
    }

    @Override
    public int updateBusinessById(int id, Business business) {
        String sql = """
                UPDATE businesses SET name = ?,
                description = ?, image = ?,
                 updated_at = now() WHERE id = ?
                """;
        int row = jdbcTemplate.update(sql, business.getName(), business.getDescription(), business.getImage(), id);

        // Update addresses
        if (business.getAddresses() != null) {
            business.getAddresses().forEach(address ->
            {
                address.setBusinessId(business.getId());
                addressDao.updateAddressById(address.getId(), address);
            });
        }

        // Update contacts
        if (business.getContacts() != null) {
            business.getContacts().forEach(contact ->
            {
                contact.setBusinessId(business.getId());
                contactDao.updateContactById(contact.getId(), contact);
            });
        }

        return row;
    }

    @Override
    public int deleteBusiness(int id, Business business) {
        String sql = "DELETE FROM businesses WHERE id = ?";
        int row = jdbcTemplate.update(sql, id);

        // Delete addresses
        if (business.getAddresses() != null) {
            System.out.println("address " + business.getAddresses().toString());
            business.getAddresses().forEach(address -> {
                addressDao.deleteAddressById(address.getId());
            });
        }

        // Delete contacts
        if (business.getContacts() != null) {
            business.getContacts().forEach(contact -> {
                contactDao.deleteContactById(contact.getId());
            });
        }

        return row;
    }
}
