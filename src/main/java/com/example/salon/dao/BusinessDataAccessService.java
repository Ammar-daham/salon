package com.example.salon.dao;

import com.example.salon.model.Business;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("postgres")
public class BusinessDataAccessService implements BusinessDao{

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
        String sqlBusiness = """
                INSERT INTO businesses (name, description)
                VALUES (?, ?)
                RETURNING id
                """;

        Long businessId = jdbcTemplate.queryForObject(
                sqlBusiness,
                Long.class,
                business.getName(),
                business.getDescription()
        );
        business.setId(businessId);

        // Insert addresses if present
        if (business.getAddresses() != null) {
            addressDao.AddAddressesForBusiness(businessId, business.getAddresses());
        }

        if (business.getContacts() != null) {
            contactDao.AddContactsForBusiness(businessId, business.getContacts());
        }
        return businessId;
    }

    public List<Business> getBusinesses() {
        String sql = "SELECT id, name, description, created_at FROM businesses";
        List<Business> businesses = jdbcTemplate.query(sql, (rs, i) ->
            new Business(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getTimestamp("created_at").toInstant()
            )
        );

        for (Business business : businesses) {
            business.setAddresses(
                    addressDao.getAddressesForBusiness(business.getId())
            );
        }

        return businesses;
    }
}
