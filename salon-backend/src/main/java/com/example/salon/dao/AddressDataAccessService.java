package com.example.salon.dao;

import com.example.salon.model.Address;
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
public class AddressDataAccessService implements AddressDao
{
	private static final String SELECT = """
			SELECT id, street, city,
			country, postal_code,
			latitude, longitude,
			created_at, updated_at,
			business_id, user_id
			FROM addresses
			""";

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
		return getAddressesForBusinesses(List.of(id)).getOrDefault(id, List.of());
	}

	@Override
	public List<Address> getAddressesForUser(Long id)
	{
		return getAddressesForUsers(List.of(id)).getOrDefault(id, List.of());
	}

	@Override
	public Map<Long, List<Address>> getAddressesForBusinesses(Collection<Long> businessIds)
	{
		return byOwner(SELECT + "WHERE business_id = ANY(?) ORDER BY id", "business_id", businessIds);
	}

	@Override
	public Map<Long, List<Address>> getAddressesForUsers(Collection<Long> userIds)
	{
		return byOwner(SELECT + "WHERE user_id = ANY(?) ORDER BY id", "user_id", userIds);
	}

	/** One query for every owner's addresses, grouped by the owner in ownerColumn. */
	private Map<Long, List<Address>> byOwner(String sql, String ownerColumn, Collection<Long> ownerIds)
	{
		Map<Long, List<Address>> byOwner = new HashMap<>();
		if (ownerIds.isEmpty()) {
			return byOwner;
		}
		jdbcTemplate.query(sql, rs -> {
			byOwner.computeIfAbsent(rs.getLong(ownerColumn), owner -> new ArrayList<>()).add(mapRow(rs));
		}, (Object) ownerIds.toArray(Long[]::new));
		return byOwner;
	}

	@Override
	public List<Address> getAllAddresses()
	{
		return jdbcTemplate.query(SELECT + "ORDER BY id", (rs, i) -> mapRow(rs));
	}

	@Override
	public Address getAddressById(int id)
	{
		return jdbcTemplate.queryForObject(SELECT + "WHERE id = ?", (rs, i) -> {
			Address address = mapRow(rs);
			// AddressService's owner-or-admin check needs the owner, so read it here.
			address.setBusinessId(rs.getObject("business_id", Long.class));
			address.setUserId(rs.getObject("user_id", Long.class));
			return address;
		}, id);
	}

	@Override
	public int updateAddressById(long id, Address address)
	{
		String sql = """
				UPDATE addresses SET street = ?,
				city = ?, country = ?, postal_code = ?,
				latitude = ?, longitude = ?, updated_at = now()
				WHERE id = ?
				""";

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
	public int deleteAddressById(long id)
	{
		String sql = "DELETE FROM addresses WHERE id = ?";
		return jdbcTemplate.update(sql, id);
	}

	private static Address mapRow(ResultSet rs) throws SQLException
	{
		Timestamp updatedAt = rs.getTimestamp("updated_at");
		return new Address(
				rs.getLong("id"),
				rs.getString("country"),
				rs.getString("city"),
				rs.getString("street"),
				rs.getString("postal_code"),
				rs.getBigDecimal("latitude"),
				rs.getBigDecimal("longitude"),
				rs.getTimestamp("created_at").toInstant(),
				updatedAt != null ? updatedAt.toInstant() : null
		);
	}
}
