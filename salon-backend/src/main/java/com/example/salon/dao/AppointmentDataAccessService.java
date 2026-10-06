package com.example.salon.dao;

import com.example.salon.model.Appointment;
import com.example.salon.model.AppointmentStatus;
import com.example.salon.model.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Repository
public class AppointmentDataAccessService implements AppointmentDao
{
    // The joins don't skip removed rows: a deleted customer, staff member or service still names the
    // appointments it was on (DB-13).
    private static final String SELECT = """
            SELECT a.id, a.starts_at, a.ends_at, a.status, a.price, a.notes, a.created_at, a.updated_at,
            a.customer_id, c.first_name AS customer_first_name, c.last_name AS customer_last_name,
            a.staff_id, u.first_name AS staff_first_name, u.last_name AS staff_last_name,
            a.service_id, sv.name AS service_name
            FROM appointments a
            JOIN customers c ON c.id = a.customer_id
            JOIN staff st ON st.id = a.staff_id
            JOIN users u ON u.id = st.user_id
            JOIN services sv ON sv.id = a.service_id
            """;

    private static final String UPCOMING = " AND status IN ('BOOKED', 'CONFIRMED') AND ends_at > now()";

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public AppointmentDataAccessService(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long addAppointment(long businessId, Booking booking, int durationMinutes, BigDecimal price, ZoneId zone)
    {
        String sql = """
                INSERT INTO appointments
                (business_id, customer_id, staff_id, service_id, starts_at, ends_at, price, notes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id;
                """;
        OffsetDateTime startsAt = onClock(booking.startsAt(), zone);
        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                businessId,
                booking.customerId(),
                booking.staffId(),
                booking.serviceId(),
                startsAt,
                // On the instant time-line, so an appointment across a daylight-saving change keeps its length.
                startsAt.plusMinutes(durationMinutes),
                price,
                booking.notes()
        );
    }

    @Override
    public List<Appointment> getAppointments(long businessId, OffsetDateTime from, OffsetDateTime to, Long staffId,
            Long customerId, ZoneId zone)
    {
        String sql = SELECT + """
                WHERE a.business_id = ?
                AND a.starts_at >= COALESCE(?::timestamptz, '-infinity')
                AND a.starts_at < COALESCE(?::timestamptz, 'infinity')
                AND a.staff_id = COALESCE(?::bigint, a.staff_id)
                AND a.customer_id = COALESCE(?::bigint, a.customer_id)
                ORDER BY a.starts_at, a.id
                """;
        return jdbcTemplate.query(sql, (rs, i) -> mapRow(rs, zone), businessId, from, to, staffId, customerId);
    }

    @Override
    public Appointment getAppointmentById(long businessId, long appointmentId, ZoneId zone)
    {
        String sql = SELECT + "WHERE a.business_id = ? AND a.id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, i) -> mapRow(rs, zone), businessId, appointmentId);
    }

    @Override
    public void lockAppointment(long businessId, long appointmentId)
    {
        jdbcTemplate.queryForList("SELECT id FROM appointments WHERE business_id = ? AND id = ? FOR NO KEY UPDATE",
                Long.class, businessId, appointmentId);
    }

    @Override
    public int updateAppointment(long businessId, long appointmentId, Booking booking, Integer durationMinutes,
            BigDecimal price, ZoneId zone)
    {
        // ends_at - starts_at on the right-hand side is the stored length: SET reads the row as it was.
        String sql = """
                UPDATE appointments SET customer_id = ?, staff_id = ?, service_id = ?,
                starts_at = ?,
                ends_at = ?::timestamptz + COALESCE(make_interval(mins => ?::int), ends_at - starts_at),
                price = COALESCE(?::numeric, price),
                notes = ?
                WHERE business_id = ? AND id = ?;
                """;
        OffsetDateTime startsAt = onClock(booking.startsAt(), zone);
        return jdbcTemplate.update(
                sql,
                booking.customerId(),
                booking.staffId(),
                booking.serviceId(),
                startsAt,
                startsAt,
                durationMinutes,
                price,
                booking.notes(),
                businessId,
                appointmentId
        );
    }

    @Override
    public int updateStatus(long businessId, long appointmentId, AppointmentStatus status)
    {
        return jdbcTemplate.update("UPDATE appointments SET status = ? WHERE business_id = ? AND id = ?",
                status.name(), businessId, appointmentId);
    }

    @Override
    public boolean hasUpcomingAppointmentsForStaff(long staffId)
    {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM appointments WHERE staff_id = ?" + UPCOMING + ")", Boolean.class,
                staffId);
    }

    @Override
    public boolean hasUpcomingAppointmentsForService(long serviceId)
    {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM appointments WHERE service_id = ?" + UPCOMING + ")", Boolean.class,
                serviceId);
    }

    @Override
    public boolean hasUpcomingAppointmentsForCustomer(long customerId)
    {
        return jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM appointments WHERE customer_id = ?" + UPCOMING + ")", Boolean.class,
                customerId);
    }

    // The same rule as time off: a wall-clock time skipped by a daylight-saving jump moves forward by the gap,
    // and a repeated one takes the earlier offset.
    private static OffsetDateTime onClock(LocalDateTime localDateTime, ZoneId zone)
    {
        return localDateTime.atZone(zone).toOffsetDateTime();
    }

    private static LocalDateTime offClock(ResultSet rs, String column, ZoneId zone) throws SQLException
    {
        return rs.getObject(column, OffsetDateTime.class).atZoneSameInstant(zone).toLocalDateTime();
    }

    private static Appointment mapRow(ResultSet rs, ZoneId zone) throws SQLException
    {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new Appointment(
                rs.getLong("id"),
                new Appointment.PersonRef(rs.getLong("customer_id"),
                        rs.getString("customer_first_name"), rs.getString("customer_last_name")),
                new Appointment.PersonRef(rs.getLong("staff_id"),
                        rs.getString("staff_first_name"), rs.getString("staff_last_name")),
                new Appointment.ServiceRef(rs.getLong("service_id"), rs.getString("service_name")),
                offClock(rs, "starts_at", zone),
                offClock(rs, "ends_at", zone),
                AppointmentStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("price"),
                rs.getString("notes"),
                rs.getTimestamp("created_at").toInstant(),
                updatedAt != null ? updatedAt.toInstant() : null
        );
    }
}
