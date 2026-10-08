package com.example.salon.dao;

import com.example.salon.model.Address;
import com.example.salon.model.Business;
import com.example.salon.model.Contact;
import com.example.salon.model.SalonService;
import com.example.salon.model.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        if (business.getCurrency() == null) business.setCurrency("EUR");
        if (business.getTimezone() == null) business.setTimezone("Europe/Berlin");

        String sql = """
                INSERT INTO businesses
                (name, description, image, status, currency, timezone)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        Long businessId = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                business.getName(),
                business.getDescription(),
                business.getImage(),
                business.getStatus().name(),
                business.getCurrency(),
                business.getTimezone()
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
                updated_at, created_at, image, status, currency, timezone
                FROM businesses
                """;
        List<Business> businesses = jdbcTemplate.query(sql, (rs, i) -> mapBusiness(rs));
        loadChildren(businesses);
        return businesses;
    }

    @Override
    public Business getBusinessById(int id) 
    {
        String sql = """
                SELECT id, name, description,
                updated_at, created_at, image, status, currency, timezone
                FROM businesses
                WHERE id = ?
                """;
        Business business = jdbcTemplate.queryForObject(sql, (rs, i) -> mapBusiness(rs), id);
        loadChildren(List.of(business));
        return business;
    }

    /** Three queries however many businesses there are, rather than three per business (BE-15). */
    private void loadChildren(List<Business> businesses)
    {
        List<Long> ids = businesses.stream().map(Business::getId).toList();
        Map<Long, List<Address>> addresses = addressDao.getAddressesForBusinesses(ids);
        Map<Long, List<Contact>> contacts = contactDao.getContactsForBusinesses(ids);
        Map<Long, List<SalonService>> services = salonServiceDao.getServicesForBusinesses(ids);
        for (Business business : businesses) {
            business.setAddresses(addresses.getOrDefault(business.getId(), new ArrayList<>()));
            business.setContacts(contacts.getOrDefault(business.getId(), new ArrayList<>()));
            business.setServices(services.getOrDefault(business.getId(), new ArrayList<>()));
        }
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
                currency = COALESCE(?, currency),
                timezone = COALESCE(?, timezone),
                 updated_at = now() WHERE id = ?
                """;
        
        return jdbcTemplate.update(
                sql,
                business.getName(),
                business.getDescription(),
                business.getImage(),
                business.getStatus() == null ? null : business.getStatus().name(),
                business.getCurrency(),
                business.getTimezone(),
                id
        );
    }

    @Override
    public int deleteBusiness(int id)
    {
        contactDao.getContactsForBusiness((long) id).forEach(contact -> contactDao.deleteContactById(contact.getId()));
        addressDao.getAddressesForBusiness((long) id).forEach(address -> addressDao.deleteAddressById(address.getId()));

        return jdbcTemplate.update("DELETE FROM businesses WHERE id = ?", id);
    }

    private Business mapBusiness(ResultSet rs) throws SQLException
    {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        Business business = new Business(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toInstant(),
                updatedAt != null ? updatedAt.toInstant() : null,
                rs.getString("image"),
                Status.valueOf(rs.getString("status"))
        );
        business.setCurrency(rs.getString("currency"));
        business.setTimezone(rs.getString("timezone"));
        return business;
    }
}
