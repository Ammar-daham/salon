package com.example.salon.dao;

import com.example.salon.model.WorkingInterval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public class StaffScheduleDataAccessService implements StaffScheduleDao {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public StaffScheduleDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<WorkingInterval> getScheduleForStaff(long staffId) {
        String sql = """
                SELECT day_of_week, starts_at, ends_at
                FROM staff_schedules
                WHERE staff_id = ?
                ORDER BY day_of_week, starts_at
                """;
        return jdbcTemplate.query(sql, (rs, i) -> new WorkingInterval(
                DayOfWeek.of(rs.getInt("day_of_week")),
                rs.getObject("starts_at", LocalTime.class),
                rs.getObject("ends_at", LocalTime.class)
        ), staffId);
    }

    @Override
    public void replaceScheduleForStaff(long staffId, List<WorkingInterval> hours) {
        // Same row lock as BusinessHoursDataAccessService, on the staff row: concurrent replacements queue up.
        jdbcTemplate.queryForList("SELECT id FROM staff WHERE id = ? FOR NO KEY UPDATE", Long.class, staffId);

        jdbcTemplate.update("DELETE FROM staff_schedules WHERE staff_id = ?", staffId);
        jdbcTemplate.batchUpdate(
                "INSERT INTO staff_schedules (staff_id, day_of_week, starts_at, ends_at) VALUES (?, ?, ?, ?)",
                hours,
                hours.size(),
                (ps, interval) -> {
                    ps.setLong(1, staffId);
                    ps.setInt(2, interval.dayOfWeek().getValue());
                    ps.setObject(3, interval.startsAt());
                    ps.setObject(4, interval.endsAt());
                }
        );
    }
}
