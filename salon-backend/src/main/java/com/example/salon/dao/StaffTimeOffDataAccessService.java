package com.example.salon.dao;

import com.example.salon.model.TimeOff;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Repository
public class StaffTimeOffDataAccessService implements StaffTimeOffDao 
{
    private static final String COLUMNS = "id, starts_at, ends_at, note, created_at, updated_at";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public StaffTimeOffDataAccessService(JdbcTemplate jdbcTemplate) 
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long addTimeOff(long staffId, TimeOff timeOff, ZoneId zone) 
    {
        String sql = """
                INSERT INTO staff_time_off (staff_id, starts_at, ends_at, note)
                VALUES (?, ?, ?, ?)
                RETURNING id;
                """;
        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                staffId,
                onClock(timeOff.startsAt(), zone),
                onClock(timeOff.endsAt(), zone),
                timeOff.note()
        );
    }

    @Override
    public List<TimeOff> getTimeOffForStaff(long staffId, ZoneId zone) 
    {
        String sql = "SELECT " + COLUMNS + " FROM staff_time_off WHERE staff_id = ? ORDER BY starts_at";
        return jdbcTemplate.query(sql, (rs, i) -> mapRow(rs, zone), staffId);
    }

    @Override
    public TimeOff getTimeOffById(long staffId, long timeOffId, ZoneId zone) 
    {
        String sql = "SELECT " + COLUMNS + " FROM staff_time_off WHERE staff_id = ? AND id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, i) -> mapRow(rs, zone), staffId, timeOffId);
    }

    @Override
    public int updateTimeOff(long staffId, long timeOffId, TimeOff timeOff, ZoneId zone) 
    {
        String sql = "UPDATE staff_time_off SET starts_at = ?, ends_at = ?, note = ? WHERE staff_id = ? AND id = ?";
        return jdbcTemplate.update(
                sql,
                onClock(timeOff.startsAt(), zone),
                onClock(timeOff.endsAt(), zone),
                timeOff.note(),
                staffId,
                timeOffId
        );
    }

    @Override
    public int deleteTimeOff(long staffId, long timeOffId) 
    {
        return jdbcTemplate.update("DELETE FROM staff_time_off WHERE staff_id = ? AND id = ?", staffId, timeOffId);
    }

    // A wall-clock time skipped by a daylight-saving jump moves forward by the gap; a repeated one takes
    // the earlier offset (ZonedDateTime's rules).
    private static OffsetDateTime onClock(LocalDateTime localDateTime, ZoneId zone) 
    {
        return localDateTime.atZone(zone).toOffsetDateTime();
    }

    private static TimeOff mapRow(ResultSet rs, ZoneId zone) throws SQLException 
    {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new TimeOff(
                rs.getLong("id"),
                rs.getObject("starts_at", OffsetDateTime.class).atZoneSameInstant(zone).toLocalDateTime(),
                rs.getObject("ends_at", OffsetDateTime.class).atZoneSameInstant(zone).toLocalDateTime(),
                rs.getString("note"),
                rs.getTimestamp("created_at").toInstant(),
                updatedAt != null ? updatedAt.toInstant() : null
        );
    }
}
