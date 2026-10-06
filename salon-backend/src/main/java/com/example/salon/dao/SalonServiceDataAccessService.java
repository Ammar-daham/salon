package com.example.salon.dao;

import com.example.salon.model.SalonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class SalonServiceDataAccessService implements SalonServiceDao 
{
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public SalonServiceDataAccessService(JdbcTemplate jdbcTemplate) 
    {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public Long addService(Long businessId, SalonService service) 
    {
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
    public List<SalonService> getServicesForBusiness(Long businessId) 
    {
        String sql = """
                SELECT id, name, description,
                duration_minutes, price, is_active,
                created_at, updated_at
                FROM services
                WHERE business_id = ?
                AND deleted_at IS NULL;
                """;

        return jdbcTemplate.query(sql, (rs, i) -> mapRow(rs), businessId);
    }

    @Override
    public List<SalonService> getServicesForStaff(long staffId) 
    {
        String sql = """
                SELECT s.id, s.name, s.description,
                s.duration_minutes, s.price, s.is_active,
                s.created_at, s.updated_at
                FROM services s
                JOIN staff_services ss ON ss.service_id = s.id
                WHERE ss.staff_id = ?
                AND s.deleted_at IS NULL
                ORDER BY s.name, s.id;
                """;

        return jdbcTemplate.query(sql, (rs, i) -> mapRow(rs), staffId);
    }

    @Override
    public SalonService getServiceById(int businessId, int serviceId) 
    {
        String sql = """
                SELECT id, name, description,
                duration_minutes, price, is_active,
                created_at, updated_at
                FROM services
                WHERE business_id = ?
                AND id = ?
                AND deleted_at IS NULL;
                """;

        return jdbcTemplate.queryForObject(sql, (rs, i) -> mapRow(rs), businessId, serviceId);
    }


    @Override
    public int updateServiceById(long id, SalonService service) 
    {
        String sql = """
                UPDATE services SET name = ?,
                description = ?, duration_minutes = ?,
                price = ?, is_active = ?,
                updated_at = now()
                WHERE id = ?
                AND deleted_at IS NULL;
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
    public int deleteServiceById(long id) 
    {
        // Soft delete (DB-13): appointments keep pointing at the service.
        String sql = "UPDATE services SET deleted_at = now() WHERE id = ? AND deleted_at IS NULL";
        return jdbcTemplate.update(sql, id);
    }

    private SalonService mapRow(ResultSet rs) throws SQLException 
    {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new SalonService(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("duration_minutes"),
                rs.getBigDecimal("price"),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at").toInstant(),
                updatedAt != null ? updatedAt.toInstant() : null
        );
    }
}
