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
    public Long addAddress(Address address)
    {
        String sql = """
                INSERT INTO addresses
                (street, city, country, postal_code, latitude, longitude, business_id, user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
                address.getBusinessId(),
                address.getUserId()
        );
        address.setId(addressId);
        return addressId;
    }

    @Override
    public List<Address> getAddressesForBusiness(Long id)
    {
        return getAddressesByColumn("business_id", id);
    }

    @Override
    public List<Address> getAddressesForUser(Long id)
    {
        return getAddressesByColumn("user_id", id);
    }

    public List<Address> getAddressesByColumn(String column, Long businessId)
    {
        String sql = """
                SELECT id, street, city, country, postal_code, latitude, longitude, created_at
                FROM addresses WHERE %s = ?;
                """.formatted(column);

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
    public List<Address> getAllAddresses()
    {
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

    @Override
    public Address getAddressById(int id) {
        String sql = "SELECT id, street, city, country, postal_code, latitude, longitude, created_at FROM addresses WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, i) ->
            new Address(
                    rs.getLong("id"),
                    rs.getString("street"),
                    rs.getString("city"),
                    rs.getString("country"),
                    rs.getString("postal_code"),
                    rs.getString("latitude"),
                    rs.getString("longitude"),
                    rs.getTimestamp("created_at").toInstant()
            ),
            id
        );
    }

    @Override
    public int updateAddressById(long id, Address address)
    {
        String sql = "UPDATE addresses SET street = ?, city = ?, country = ?, postal_code = ?, latitude = ?, longitude = ? WHERE id = ?";
        return jdbcTemplate.update(
                sql,
                address.getStreet(),
                address.getCity(),
                address.getCountry(),
                address.getPostalCode(),
                address.getLatitude(),
                address.getLongitude(),
                id);
    }

    @Override
    public int deleteAddressById(int id)
    {
        String sql = "DELETE FROM addresses WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
