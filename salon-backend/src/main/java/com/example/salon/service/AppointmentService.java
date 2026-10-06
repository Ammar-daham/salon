package com.example.salon.service;

import com.example.salon.dao.AppointmentDao;
import com.example.salon.dao.CustomerDao;
import com.example.salon.dao.SalonServiceDao;
import com.example.salon.dao.StaffDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Appointment;
import com.example.salon.model.AppointmentStatus;
import com.example.salon.model.Booking;
import com.example.salon.model.SalonService;
import com.example.salon.model.Staff;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * A salon's appointment book. Its ADMINs and EMPLOYEEs (and a SUPER_ADMIN) can see, book and change any of its
 * appointments, as a front desk does. A booking must name the salon's own current customer, an active staff
 * member and an active service they perform, and the staff member can't be booked twice at once (the database
 * refuses that). Whether the time suits opening hours, shifts and time off is left to availability.
 *
 * An appointment is never deleted: it is cancelled, and stays in the history like every other one that has ended.
 */
@Service
public class AppointmentService
{
    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentDao appointmentDao;
    private final CustomerDao customerDao;
    private final StaffDao staffDao;
    private final SalonServiceDao salonServiceDao;
    private final BusinessService businessService;

    @Autowired
    public AppointmentService(AppointmentDao appointmentDao, CustomerDao customerDao, StaffDao staffDao,
            SalonServiceDao salonServiceDao, BusinessService businessService)
    {
        this.appointmentDao = appointmentDao;
        this.customerDao = customerDao;
        this.staffDao = staffDao;
        this.salonServiceDao = salonServiceDao;
        this.businessService = businessService;
    }

    @Transactional
    public Appointment book(long businessId, Booking booking, AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        ZoneId zone = zoneOf(businessId);
        requireCustomer(businessId, booking.customerId());
        requireActiveStaff(businessId, booking.staffId());
        SalonService service = requireActiveService(businessId, booking.serviceId());
        requirePerforms(booking.staffId(), booking.serviceId());

        Long id;
        try {
            id = appointmentDao.addAppointment(businessId, booking, service.getDuration(), service.getPrice(), zone);
        } catch (DataIntegrityViolationException ex) {
            throw doubleBookingOrRethrow(ex, booking.staffId());
        }
        log.info("Booked appointment {} with staff {} in business {}", id, booking.staffId(), businessId);
        return findAppointment(businessId, id, zone);
    }

    /** from and to are dates on the salon's clock, both included. */
    public List<Appointment> getAppointments(long businessId, LocalDate from, LocalDate to, Long staffId,
            Long customerId, AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        ZoneId zone = zoneOf(businessId);
        if (from != null && to != null && to.isBefore(from))
            throw new BaseException("to must not be before from.", ErrorCode.BAD_REQUEST);
        OffsetDateTime start = from == null ? null : from.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = to == null ? null : to.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        return appointmentDao.getAppointments(businessId, start, end, staffId, customerId, zone);
    }

    public Appointment getAppointmentById(long businessId, long appointmentId, AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        return findAppointment(businessId, appointmentId, zoneOf(businessId));
    }

    /**
     * Replaces who, what and when. Only what changes is checked, so an appointment stays editable after its staff
     * member or service is deactivated. The length and price are the service's again only if the service changes.
     */
    @Transactional
    public Appointment updateAppointment(long businessId, long appointmentId, Booking booking,
            AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        ZoneId zone = zoneOf(businessId);
        Appointment existing = lockAndFind(businessId, appointmentId, zone);
        if (!existing.status().isOpen())
            throw new BaseException("A " + existing.status() + " appointment can't be changed.",
                    ErrorCode.DUPLICATE_RESOURCE);

        boolean newStaff = booking.staffId() != existing.staff().id();
        boolean newService = booking.serviceId() != existing.service().id();
        if (booking.customerId() != existing.customer().id())
            requireCustomer(businessId, booking.customerId());
        if (newStaff)
            requireActiveStaff(businessId, booking.staffId());
        SalonService service = newService ? requireActiveService(businessId, booking.serviceId()) : null;
        if (newStaff || newService)
            requirePerforms(booking.staffId(), booking.serviceId());

        try {
            appointmentDao.updateAppointment(businessId, appointmentId, booking,
                    service == null ? null : service.getDuration(), service == null ? null : service.getPrice(), zone);
        } catch (DataIntegrityViolationException ex) {
            throw doubleBookingOrRethrow(ex, booking.staffId());
        }
        log.info("Updated appointment {} in business {}", appointmentId, businessId);
        return findAppointment(businessId, appointmentId, zone);
    }

    /**
     * Moves it along BOOKED → CONFIRMED → COMPLETED / CANCELLED / NO_SHOW. Asking for the status it already has
     * changes nothing. It can only be COMPLETED or a NO_SHOW once it has started.
     */
    @Transactional
    public Appointment changeStatus(long businessId, long appointmentId, AppointmentStatus status,
            AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        ZoneId zone = zoneOf(businessId);
        Appointment existing = lockAndFind(businessId, appointmentId, zone);
        if (status == existing.status())
            return existing;
        if (!existing.status().canBecome(status))
            throw new BaseException("A " + existing.status() + " appointment can't become " + status + ".",
                    ErrorCode.DUPLICATE_RESOURCE);
        boolean happened = status == AppointmentStatus.COMPLETED || status == AppointmentStatus.NO_SHOW;
        if (happened && existing.startsAt().isAfter(LocalDateTime.now(zone)))
            throw new BaseException("An appointment can't be marked " + status + " before it starts.",
                    ErrorCode.DUPLICATE_RESOURCE);

        appointmentDao.updateStatus(businessId, appointmentId, status);
        log.info("Appointment {} in business {} went from {} to {}", appointmentId, businessId, existing.status(),
                status);
        return findAppointment(businessId, appointmentId, zone);
    }

    private ZoneId zoneOf(long businessId)
    {
        return ZoneId.of(businessService.getBusinessById((int) businessId).getTimezone());
    }

    /** Locks it first, so of two changes at once the second sees what the first did, e.g. that it was cancelled. */
    private Appointment lockAndFind(long businessId, long appointmentId, ZoneId zone)
    {
        appointmentDao.lockAppointment(businessId, appointmentId);
        return findAppointment(businessId, appointmentId, zone);
    }

    private Appointment findAppointment(long businessId, long appointmentId, ZoneId zone)
    {
        try {
            return appointmentDao.getAppointmentById(businessId, appointmentId, zone);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Appointment with id " + appointmentId + " not found.", ErrorCode.NOT_FOUND);
        }
    }

    private void requireCustomer(long businessId, long customerId)
    {
        try {
            customerDao.getCustomerById(businessId, customerId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Customer with id " + customerId + " is not a customer of this business.",
                    ErrorCode.BAD_REQUEST);
        }
    }

    private void requireActiveStaff(long businessId, long staffId)
    {
        Staff staff;
        try {
            staff = staffDao.getStaffById(businessId, staffId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Staff with id " + staffId + " is not on the staff of this business.",
                    ErrorCode.BAD_REQUEST);
        }
        if (!staff.isActive())
            throw new BaseException("Staff with id " + staffId + " is not active.", ErrorCode.BAD_REQUEST);
    }

    private SalonService requireActiveService(long businessId, long serviceId)
    {
        SalonService service;
        try {
            service = salonServiceDao.getServiceById((int) businessId, (int) serviceId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Service with id " + serviceId + " is not offered by this business.",
                    ErrorCode.BAD_REQUEST);
        }
        if (!service.isActive())
            throw new BaseException("Service with id " + serviceId + " is not active.", ErrorCode.BAD_REQUEST);
        return service;
    }

    private void requirePerforms(long staffId, long serviceId)
    {
        boolean performs = salonServiceDao.getServicesForStaff(staffId).stream()
                .anyMatch(service -> service.getId() == serviceId);
        if (!performs)
            throw new BaseException("Staff with id " + staffId + " does not perform service " + serviceId + ".",
                    ErrorCode.BAD_REQUEST);
    }

    private static RuntimeException doubleBookingOrRethrow(DataIntegrityViolationException ex, long staffId)
    {
        if (ex.getMessage() == null || !ex.getMessage().contains("appointments_no_double_booking"))
            return ex;
        return new BaseException("Staff with id " + staffId + " already has an appointment at that time.",
                ErrorCode.DUPLICATE_RESOURCE);
    }
}
