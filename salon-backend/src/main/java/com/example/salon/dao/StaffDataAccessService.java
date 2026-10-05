package com.example.salon.dao;

import com.example.salon.model.Staff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public class StaffDataAccessService implements StaffDao {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public StaffDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long addStaff(Staff staff) {
        String sql = """
                INSERT INTO staff
                (user_id, business_id, title, is_active, hired_at, calendar_colour)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id;
                """;

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                staff.getUserId(),
                staff.getBusinessId(),
                staff.getTitle(),
                staff.isActive(),
                staff.getHiredAt(),
                staff.getCalendarColour()
        );
    }

    @Override
    public List<Staff> getStaffForBusiness(Long businessId) {
        String sql = """
                SELECT id, title, is_active, user_id,
                hired_at, calendar_colour,
                created_at, updated_at
                FROM staff
                WHERE business_id = ?;
                """;

        return jdbcTemplate.query(sql, (rs, i) -> mapRow(rs), businessId);
    }

    @Override
    public Staff getStaffById(long businessId, long staffId) {
        String sql = """
                SELECT id, title, is_active, user_id,
                hired_at, calendar_colour,
                created_at, updated_at
                FROM staff
                WHERE business_id = ?
                AND id = ?;
                """;

        return jdbcTemplate.queryForObject(sql, (rs, i) -> mapRow(rs), businessId, staffId);
    }

    @Override
    public int updateStaffById(long id, Staff staff) {
        String sql = """
                UPDATE staff SET title = ?,
                is_active = ?, hired_at = ?,
                calendar_colour = ?,
                updated_at = now()
                WHERE id = ?;
                """;
        return jdbcTemplate.update(
                sql,
                staff.getTitle(),
                staff.isActive(),
                staff.getHiredAt(),
                staff.getCalendarColour(),
                id
        );
    }

    @Override
    public int deleteStaffById(long id) {
        String sql = "DELETE FROM staff WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    @Override
    public void replaceServicesOfStaff(long businessId, long staffId, Collection<Long> serviceIds) {
        // Locks the staff row so concurrent replacements run one after the other instead of the second
        // one inserting rows the first just wrote and failing on the primary key.
        jdbcTemplate.queryForList("SELECT id FROM staff WHERE id = ? FOR NO KEY UPDATE", Long.class, staffId);

        jdbcTemplate.update("DELETE FROM staff_services WHERE staff_id = ?", staffId);
        jdbcTemplate.batchUpdate(
                "INSERT INTO staff_services (business_id, staff_id, service_id) VALUES (?, ?, ?)",
                serviceIds,
                serviceIds.size(),
                (ps, serviceId) -> {
                    ps.setLong(1, businessId);
                    ps.setLong(2, staffId);
                    ps.setLong(3, serviceId);
                }
        );
    }

    private Staff mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        LocalDate hiredAt = rs.getObject("hired_at", LocalDate.class);
        return new Staff(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getBoolean("is_active"),
                rs.getLong("user_id"),
                hiredAt,
                rs.getString("calendar_colour"),
                rs.getTimestamp("created_at").toInstant(),
                updatedAt != null ? updatedAt.toInstant() : null
        );
    }
}
