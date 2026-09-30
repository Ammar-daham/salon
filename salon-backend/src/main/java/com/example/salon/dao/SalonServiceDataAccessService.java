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
    public Long addService(Long businessId, SalonService service) {
        String sql = """
                INSERT INTO services
                (business_id, name, description,
                duration_minutes, price, is_active)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id;
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                businessId,
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
                SELECT id, name, description,
                duration_minutes, price, is_active,
                created_at, updated_at
                FROM services
                WHERE business_id = ?;
                """;

        return jdbcTemplate.query(sql, (rs, i) -> {
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            return new SalonService(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getInt("duration_minutes"),
                    rs.getDouble("price"),
                    rs.getBoolean("is_active"),
                    rs.getTimestamp("created_at").toInstant(),
                    updatedAt != null ? updatedAt.toInstant() : null
            );
        }, businessId);
    }

    @Override
    public SalonService getServiceById(int businessId, int serviceId) {
        String sql = """
                SELECT id, name, description,
                duration_minutes, price, is_active,
                created_at, updated_at
                FROM services
                WHERE business_id = ?
                AND id = ?;
                """;

        return jdbcTemplate.queryForObject(sql, (rs, i) -> {
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    return new SalonService(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getInt("duration_minutes"),
                            rs.getDouble("price"),
                            rs.getBoolean("is_active"),
                            rs.getTimestamp("created_at").toInstant(),
                            updatedAt != null ? updatedAt.toInstant() : null
                    );
                }, businessId, serviceId
        );
    }


    @Override
    public int updateServiceById(long id, SalonService service) {
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
    public int deleteServiceById(long id) {
        String sql = "DELETE FROM services WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
