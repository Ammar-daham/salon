package com.example.salon.dao;

import com.example.salon.model.WorkingInterval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class AvailabilityDataAccessService implements AvailabilityDao
{
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AvailabilityDataAccessService(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Performer> getStaffPerformingService(long businessId, long serviceId)
    {
        String sql = """
                SELECT st.id, u.first_name, u.last_name, st.is_active
                FROM staff st
                JOIN users u ON u.id = st.user_id
                JOIN staff_services ss ON ss.staff_id = st.id
                WHERE st.business_id = ?
                AND ss.service_id = ?
                AND st.deleted_at IS NULL
                ORDER BY u.first_name, u.last_name, st.id
                """;
        return jdbcTemplate.query(sql, (rs, i) -> new Performer(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getBoolean("is_active")
        ), businessId, serviceId);
    }

    @Override
    public List<Shift> getShiftsForBusiness(long businessId)
    {
        String sql = """
                SELECT sch.staff_id, sch.day_of_week, sch.starts_at, sch.ends_at
                FROM staff_schedules sch
                JOIN staff st ON st.id = sch.staff_id
                WHERE st.business_id = ?
                AND st.deleted_at IS NULL
                ORDER BY sch.staff_id, sch.day_of_week, sch.starts_at
                """;
        return jdbcTemplate.query(sql, (rs, i) -> new Shift(
                rs.getLong("staff_id"),
                new WorkingInterval(
                        DayOfWeek.of(rs.getInt("day_of_week")),
                        rs.getObject("starts_at", LocalTime.class),
                        rs.getObject("ends_at", LocalTime.class))
        ), businessId);
    }

    @Override
    public List<BusyTime> getBusyTimesForBusiness(long businessId, OffsetDateTime from, OffsetDateTime to)
    {
        // The same appointments the double-booking constraint counts: a cancelled or missed one frees its time.
        String sql = """
                SELECT t.staff_id, t.starts_at, t.ends_at
                FROM staff_time_off t
                JOIN staff st ON st.id = t.staff_id
                WHERE st.business_id = ?
                AND t.starts_at < ? AND t.ends_at > ?
                UNION ALL
                SELECT a.staff_id, a.starts_at, a.ends_at
                FROM appointments a
                WHERE a.business_id = ?
                AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
                AND a.starts_at < ? AND a.ends_at > ?
                """;
        return jdbcTemplate.query(sql, (rs, i) -> new BusyTime(
                rs.getLong("staff_id"),
                rs.getObject("starts_at", OffsetDateTime.class).toInstant(),
                rs.getObject("ends_at", OffsetDateTime.class).toInstant()
        ), businessId, to, from, businessId, to, from);
    }
}
