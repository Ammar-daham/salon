package com.example.salon.dao;

import com.example.salon.model.Customer;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class CustomerDataAccessService implements CustomerDao {
    private static final String COLUMNS = """
            c.id, c.business_id, b.name AS business_name, c.user_id, c.first_name, c.last_name, c.email, c.phone,
            c.notes, c.marketing_consent, c.created_at, c.updated_at
            """;
    private static final String FROM = "customers c JOIN businesses b ON b.id = c.business_id";

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
    public Page<Customer> getCustomers(Filter filter, Sort<SortBy> sort, PageQuery page) {
        ListQuery query = new ListQuery().where("c.deleted_at IS NULL");
        if (filter.businessId() != null)
            query.where("c.business_id = ?", filter.businessId());
        String term = ListQuery.containing(filter.search());
        if (term != null) {
            query.where("(concat_ws(' ', c.first_name, c.last_name) ILIKE ? OR c.email ILIKE ? OR c.phone ILIKE ?)",
                    term, term, term);
        }
        List<String> columns = switch (sort.key()) {
            case NAME -> List.of("lower(c.last_name)", "lower(c.first_name)");
            case CREATED_AT -> List.of("c.created_at");
            case BUSINESS_NAME -> List.of("lower(b.name)", "lower(c.last_name)", "lower(c.first_name)");
        };
        return query.page(jdbcTemplate, COLUMNS, FROM, ListQuery.orderBy(columns, sort.descending(), "c.id"), page,
                (rs, i) -> mapRow(rs));
    }

    @Override
    public Customer getCustomerById(long businessId, long customerId) {
        String sql = "SELECT " + COLUMNS + " FROM " + FROM
                + " WHERE c.business_id = ? AND c.id = ? AND c.deleted_at IS NULL";
        return jdbcTemplate.queryForObject(sql, (rs, i) -> mapRow(rs), businessId, customerId);
    }

    @Override
    public Optional<Customer> findCustomer(long customerId) {
        String sql = "SELECT " + COLUMNS + " FROM " + FROM + " WHERE c.id = ? AND c.deleted_at IS NULL";
        return jdbcTemplate.query(sql, (rs, i) -> mapRow(rs), customerId).stream().findFirst();
    }

    @Override
    public int updateCustomerById(long businessId, long customerId, Customer customer) {
        String sql = """
                UPDATE customers SET first_name = ?, last_name = ?,
                email = ?, phone = ?, notes = ?, marketing_consent = ?
                WHERE business_id = ? AND id = ? AND deleted_at IS NULL;
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
        // Soft delete: appointments keep pointing at the customer.
        return jdbcTemplate.update(
                "UPDATE customers SET deleted_at = now() WHERE business_id = ? AND id = ? AND deleted_at IS NULL",
                businessId, customerId);
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        Customer customer = new Customer(
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
        customer.setBusinessName(rs.getString("business_name"));
        return customer;
    }
}
