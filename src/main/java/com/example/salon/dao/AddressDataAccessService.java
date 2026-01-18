package com.example.salon.dao;

import com.example.salon.model.Address;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AddressDataAccessService implements AddressDao
{
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AddressDataAccessService(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public void addAddressForBusiness(Long businessId, Address address)
    {
        String sql = """
                INSERT INTO addresses
                (street, city, country, postal_code, latitude, longitude, business_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id;
                """;

        Long addressId = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                address.getStreet(),
                address.getCity(),
                address.getCountry(),
                address.getPostalCode(),
                address.getLatitude(),
                address.getLongitude(),
                businessId
        );
        address.setId(addressId);
    }

    @Override
    public void AddAddressesForBusiness(Long businessId, List<Address> addresses)
    {
        for (Address address : addresses) {
            addAddressForBusiness(businessId, address);
        }
    }

    @Override
    public List<Address> getAddressesForBusiness(Long businessId)
    {
        String sql = """
                SELECT id, street, city, country, postal_code, latitude, longitude, created_at
                FROM addresses WHERE business_id = ?;
                """;

        return jdbcTemplate.query(sql, (rs, i) ->
             new Address(
                rs.getLong("id"),
                rs.getString("street"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getString("postal_code"),
                rs.getString("latitude"),
                rs.getString("longitude"),
                rs.getTimestamp("created_at").toInstant()
            ),businessId
        );
    }

    @Override
    public List<Address> getAllAddresses() {
        String sql = "SELECT id, street, city, country, postal_code, latitude, longitude, created_at from addresses";
        return jdbcTemplate.query(sql, (rs, i) ->
            new Address(
                rs.getLong("id"),
                rs.getString("street"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getString("postal_code"),
                rs.getString("latitude"),
                rs.getString("longitude"),
                rs.getTimestamp("created_at").toInstant()
            )
        );
    }
}
