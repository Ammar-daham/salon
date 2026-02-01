package com.example.salon.dao;

import com.example.salon.model.SalonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SalonServiceDataAccessService implements SalonServiceDao {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public SalonServiceDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public Long addService(SalonService service) {
        String sql = """
                INSERT INTO services
                (name, description,
                duration_minutes, price, is_active)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id;
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                service.getName(),
                service.getDescription(),
                service.getDuration(),
                service.getPrice(),
                service.isActive()
        );
    }

    @Override
    public List<SalonService> getAllServices() {
        return List.of();
    }

    @Override
    public SalonService getServiceById(long id) {
        return null;
    }

    @Override
    public int updateService(int id, SalonService service) {
        return 0;
    }

    @Override
    public int deleteService(int id) {
        return 0;
    }
}
