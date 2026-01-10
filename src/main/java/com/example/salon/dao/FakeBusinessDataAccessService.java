package com.example.salon.dao;

import com.example.salon.model.Business;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository("postgres")
public class FakeBusinessDataAccessService implements BusinessDao{

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FakeBusinessDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static List<Business> DB = new ArrayList<>();

    @Override
    public int insertBusiness(Business business) {
        String sql = """
            INSERT INTO business (name, description)
            VALUES (?, ?)
        """;

        return jdbcTemplate.update(
                sql,
                business.getName(),
                business.getDescription()
        );
    }

    public List<Business> getBusinesses() {
        String sql = "select * from business";
        return jdbcTemplate.query(sql, (rs, i) -> {
            return new Business(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getTimestamp("created_at").toInstant()
            );
        });
    }
}
