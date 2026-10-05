package com.example.salon.dao;

import com.example.salon.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class CustomerDataAccessService implements CustomerDao {
    private static final String COLUMNS = """
            id, business_id, user_id, first_name, last_name, email, phone,
            notes, marketing_consent, created_at, updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CustomerDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long addCustomer(Customer customer) {
        String sql = """
                INSERT INTO customers
                (business_id, first_name, last_name, email, phone, notes, marketing_consent)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id;
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                customer.getBusinessId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getNotes(),
                customer.isMarketingConsent()
        );
    }

    @Override
    public List<Customer> getCustomersForBusiness(long businessId) {
        String sql = "SELECT " + COLUMNS + " FROM customers WHERE business_id = ? ORDER BY last_name, first_name, id";
        return jdbcTemplate.query(sql, (rs, i) -> mapRow(rs), businessId);
    }

    @Override
    public Customer getCustomerById(long businessId, long customerId) {
        String sql = "SELECT " + COLUMNS + " FROM customers WHERE business_id = ? AND id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, i) -> mapRow(rs), businessId, customerId);
    }

    @Override
    public int updateCustomerById(long businessId, long customerId, Customer customer) {
        String sql = """
                UPDATE customers SET first_name = ?, last_name = ?,
                email = ?, phone = ?, notes = ?, marketing_consent = ?
                WHERE business_id = ? AND id = ?;
                """;
        return jdbcTemplate.update(
                sql,
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getNotes(),
                customer.isMarketingConsent(),
                businessId,
                customerId
        );
    }

    @Override
    public int deleteCustomerById(long businessId, long customerId) {
        return jdbcTemplate.update("DELETE FROM customers WHERE business_id = ? AND id = ?", businessId, customerId);
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new Customer(
                rs.getLong("id"),
                rs.getLong("business_id"),
                rs.getObject("user_id", Long.class),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("notes"),
                rs.getBoolean("marketing_consent"),
                rs.getTimestamp("created_at").toInstant(),
                updatedAt != null ? updatedAt.toInstant() : null
        );
    }
}
