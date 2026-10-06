package com.example.salon.dao;

import com.example.salon.model.Appointment;
import com.example.salon.model.Booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** zone is the salon's time zone: Appointment's date-times are on its clock, the table stores instants. */
public interface AppointmentDao {
    /** durationMinutes and price are the booked service's. */
    Long addAppointment(long businessId, Booking booking, int durationMinutes, BigDecimal price, ZoneId zone);

    /** Ordered by start. Appointments starting in [from, to); a null bound, staffId or customerId doesn't narrow. */
    List<Appointment> getAppointments(long businessId, OffsetDateTime from, OffsetDateTime to, Long staffId,
            Long customerId, ZoneId zone);

    Appointment getAppointmentById(long businessId, long appointmentId, ZoneId zone);

    /** A null durationMinutes and price keep the stored length and price. */
    int updateAppointment(long businessId, long appointmentId, Booking booking, Integer durationMinutes,
            BigDecimal price, ZoneId zone);

    /** Upcoming: BOOKED or CONFIRMED, and not over yet. */
    boolean hasUpcomingAppointmentsForStaff(long staffId);

    boolean hasUpcomingAppointmentsForService(long serviceId);

    boolean hasUpcomingAppointmentsForCustomer(long customerId);
}
