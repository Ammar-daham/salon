package com.example.salon.dao;

import com.example.salon.model.Appointment;
import com.example.salon.model.AppointmentStatus;
import com.example.salon.model.Booking;
import com.example.salon.paging.Page;
import com.example.salon.paging.PageQuery;
import com.example.salon.paging.Sort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** zone is the salon's time zone: Appointment's date-times are on its clock, the table stores instants. */
public interface AppointmentDao {
    enum SortBy { STARTS_AT }

    /** Appointments starting in [from, to); a null bound, staffId, customerId or status doesn't narrow. */
    record Filter(long businessId, OffsetDateTime from, OffsetDateTime to, Long staffId, Long customerId,
            AppointmentStatus status) {}

    /** durationMinutes and price are the booked service's. */
    Long addAppointment(long businessId, Booking booking, int durationMinutes, BigDecimal price, ZoneId zone);

    Page<Appointment> getAppointments(Filter filter, Sort<SortBy> sort, PageQuery page, ZoneId zone);

    Appointment getAppointmentById(long businessId, long appointmentId, ZoneId zone);

    /** Call inside a transaction: holds the row until it ends, so changes to one appointment run one at a time. */
    void lockAppointment(long businessId, long appointmentId);

    /** A null durationMinutes and price keep the stored length and price. */
    int updateAppointment(long businessId, long appointmentId, Booking booking, Integer durationMinutes,
            BigDecimal price, ZoneId zone);

    int updateStatus(long businessId, long appointmentId, AppointmentStatus status);

    /** Upcoming: BOOKED or CONFIRMED, and not over yet. */
    boolean hasUpcomingAppointmentsForStaff(long staffId);

    boolean hasUpcomingAppointmentsForService(long serviceId);

    boolean hasUpcomingAppointmentsForCustomer(long customerId);
}
