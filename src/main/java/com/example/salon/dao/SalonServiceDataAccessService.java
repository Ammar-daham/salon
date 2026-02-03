package com.example.salon.dao;

import com.example.salon.model.SalonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
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
    public List<SalonService> getServicesForBusiness(Long businessId) {
        String sql = """
                SELECT s.id,
                s.name, s.description,
                s.duration_minutes,
                s.price, is_active,
                s.created_at, s.updated_at
                FROM services s
                JOIN business_service bs ON bs.service_id = s.id
                WHERE bs.business_id = ?;
                """;

        return jdbcTemplate.query(sql, (rs, i) -> {
            Timestamp updated_at = rs.getTimestamp("updated_at");
            return new SalonService(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("duration_minutes"),
                    rs.getDouble("price"),
                    rs.getBoolean("is_active"),
                    rs.getTimestamp("created_at").toInstant(),
                    updated_at != null ? updated_at.toInstant() : null
            );
        }, businessId);
    }

    ;

    @Override
    public SalonService getServiceById(long id) {
        return null;
    }

    @Override
    public int updateService(long id, SalonService service) {
        String sql = """
                UPDATE services SET name = ?,
                description = ?, duration_minutes = ?,
                price = ?, is_active = ?,
                updated_at = now()
                WHERE id = ?;
                """;
        return jdbcTemplate.update(
                sql,
                service.getName(),
                service.getDescription(),
                service.getDuration(),
                service.getPrice(),
                service.isActive(),
                id
        );
    }

    @Override
    public int deleteService(int id) {
        return 0;
    }
}
