package com.example.salon.dao;

import com.example.salon.model.Business;
import com.example.salon.model.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class BusinessDataAccessService implements BusinessDao 
{
    private final JdbcTemplate jdbcTemplate;
    private final AddressDao addressDao;
    private final ContactDao contactDao;
    private final SalonServiceDao salonServiceDao;

    @Autowired
    public BusinessDataAccessService(JdbcTemplate jdbcTemplate,
                                     AddressDao addressDao,
                                     ContactDao contactDao,
                                     SalonServiceDao salonServiceDao) 
    {
        this.jdbcTemplate = jdbcTemplate;
        this.addressDao = addressDao;
        this.contactDao = contactDao;
        this.salonServiceDao = salonServiceDao;
    }

    @Override
    public Long addBusiness(Business business) 
    {

        if (business.getStatus() == null) business.setStatus(Status.PENDING);

        String sql = """
                INSERT INTO businesses
                (name, description, image, status)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;

        Long businessId = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                business.getName(),
                business.getDescription(),
                business.getImage(),
                business.getStatus().name()
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

    public List<Business> getBusinesses() 
    {
        String sql = """
                SELECT id, name, description,
                updated_at, created_at, image, status
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
                    rs.getString("image"),
                    Status.valueOf(rs.getString("status"))
            );
        });

        for (Business business : businesses) {
            business.setAddresses(addressDao.getAddressesForBusiness(business.getId()));
            business.setContacts(contactDao.getContactsForBusiness(business.getId()));
            business.setServices(salonServiceDao.getServicesForBusiness(business.getId()));
        }

        return businesses;
    }

    @Override
    public Business getBusinessById(int id) 
    {
        String sql = """
                SELECT id, name, description,
                updated_at, created_at, image, status
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
                            rs.getString("image"),
                            Status.valueOf(rs.getString("status"))
                    );
                }, id
        );

        business.setAddresses(addressDao.getAddressesForBusiness(business.getId()));
        business.setContacts(contactDao.getContactsForBusiness(business.getId()));
        business.setServices(salonServiceDao.getServicesForBusiness(business.getId()));

        return business;
    }

    @Override
    public int updateBusinessById(int id, Business business) 
    {
        // status and image use COALESCE so that a PUT which omits them preserves the
        // stored value. Without it, omitting status made approve/reject/suspend a silent
        // no-op that still returned 200, and omitting image violated image NOT NULL.
        String sql = """
                UPDATE businesses SET name = ?,
                description = ?,
                image = COALESCE(?, image),
                status = COALESCE(?, status),
                 updated_at = now() WHERE id = ?
                """;
        // BE-06/BE-07/BE-21: this update touches only the business's own columns. It no longer walks
        // client-supplied addresses/contacts/services: doing so let a caller edit any child row by id
        // regardless of owner (IDOR), crashed on a new child whose id is null, and duplicated the same
        // loop in UserDataAccessService. Children are edited through their own ownership-checked
        // endpoints (/addresses, /contacts, /businesses/{id}/services).
        return jdbcTemplate.update(
                sql,
                business.getName(),
                business.getDescription(),
                business.getImage(),
                business.getStatus() == null ? null : business.getStatus().name(),
                id
        );
    }

    @Override
    public int deleteBusiness(int id) 
    {
        // delete the children that actually belong to this business, read from the canonical
        // record - never from a client-supplied body, which could be empty and orphan them. Children
        // must go first: the addresses/contacts FKs are ON DELETE SET NULL, so deleting the business
        // first would leave those rows behind (and a contact's globally-unique value burned forever).
        contactDao.getContactsForBusiness((long) id).forEach(contact -> contactDao.deleteContactById(contact.getId()));
        addressDao.getAddressesForBusiness((long) id).forEach(address -> addressDao.deleteAddressById(address.getId()));
        salonServiceDao.getServicesForBusiness((long) id).forEach(service -> salonServiceDao.deleteServiceById(service.getId()));

        return jdbcTemplate.update("DELETE FROM businesses WHERE id = ?", id);
    }
}
