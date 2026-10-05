package com.example.salon.dao;

import com.example.salon.model.OpeningInterval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public class BusinessHoursDataAccessService implements BusinessHoursDao {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public BusinessHoursDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OpeningInterval> getHoursForBusiness(long businessId) {
        String sql = """
                SELECT day_of_week, opens_at, closes_at
                FROM business_hours
                WHERE business_id = ?
                ORDER BY day_of_week, opens_at
                """;
        return jdbcTemplate.query(sql, (rs, i) -> new OpeningInterval(
                DayOfWeek.of(rs.getInt("day_of_week")),
                rs.getObject("opens_at", LocalTime.class),
                rs.getObject("closes_at", LocalTime.class)
        ), businessId);
    }

    /** Call inside a transaction: the old week is deleted before the new one is written. */
    @Override
    public void replaceHoursForBusiness(long businessId, List<OpeningInterval> hours) {
        // Locks the salon's row so two concurrent replacements run one after the other. Without it, both
        // delete the same old rows, both insert, and the second one fails on business_hours_no_overlap.
        jdbcTemplate.queryForList("SELECT id FROM businesses WHERE id = ? FOR NO KEY UPDATE", Long.class, businessId);

        jdbcTemplate.update("DELETE FROM business_hours WHERE business_id = ?", businessId);
        jdbcTemplate.batchUpdate(
                "INSERT INTO business_hours (business_id, day_of_week, opens_at, closes_at) VALUES (?, ?, ?, ?)",
                hours,
                hours.size(),
                (ps, interval) -> {
                    ps.setLong(1, businessId);
                    ps.setInt(2, interval.dayOfWeek().getValue());
                    ps.setObject(3, interval.opensAt());
                    ps.setObject(4, interval.closesAt());
                }
        );
    }
}
