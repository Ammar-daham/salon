package com.example.salon.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BusinessServiceDataAccessService implements BusinessServiceDao {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public BusinessServiceDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void linkServiceToBusiness(Long businessId, Long serviceId) {
        String sql = """
                INSERT INTO business_service(business_id, service_id)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(sql, businessId, serviceId);
    }
}
