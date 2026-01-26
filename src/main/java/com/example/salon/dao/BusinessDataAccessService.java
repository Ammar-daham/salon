package com.example.salon.dao;

import com.example.salon.model.Business;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
    @Transactional // Ensures atomicity: either both inserts succeed or rollback
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
        List<Business> businesses = jdbcTemplate.query(sql, (rs, i) ->
                new Business(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant(),
                        rs.getString("image")
                )
        );

        for (Business business : businesses) {
            business.setAddresses(addressDao.getAddressesForBusiness(business.getId()));
            business.setContacts(contactDao.getContactsForBusiness(business.getId()));
        }

        return businesses;
    }

    @Override
    public int updateBusinessByid(int id, Business business) {
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
    public int deleteBusiness(int id) {
        String sql = "DELETE FROM businesses WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
