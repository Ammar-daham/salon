package com.example.salon.service;

import com.example.salon.dao.AvailabilityDao;
import com.example.salon.dao.AvailabilityDao.BusyTime;
import com.example.salon.dao.AvailabilityDao.Performer;
import com.example.salon.dao.AvailabilityDao.Shift;
import com.example.salon.dao.BusinessHoursDao;
import com.example.salon.dao.SalonServiceDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Availability;
import com.example.salon.model.Availability.Slot;
import com.example.salon.model.Availability.StaffSlots;
import com.example.salon.model.Business;
import com.example.salon.model.OpeningInterval;
import com.example.salon.model.SalonService;
import com.example.salon.model.WorkingInterval;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * Open slots for a service, per active staff member who performs it: inside the salon's opening hours and the
 * staff member's shift that day, clear of their time off and of appointments that still hold time, and not
 * already past. Slots start on the quarter hours of the salon's clock.
 *
 * This is advice for the booking form. Booking itself only refuses a double-booking, so staff can still fit
 * someone in after hours. Only the salon's staff (and a SUPER_ADMIN) can see it: the gaps show when people are away.
 */
@Service
public class AvailabilityService
{
    private static final int GRID_MINUTES = 15;
    private static final int MAX_DAYS = 31;

    private final AvailabilityDao availabilityDao;
    private final BusinessHoursDao businessHoursDao;
    private final SalonServiceDao salonServiceDao;
    private final BusinessService businessService;

    @Autowired
    public AvailabilityService(AvailabilityDao availabilityDao, BusinessHoursDao businessHoursDao,
            SalonServiceDao salonServiceDao, BusinessService businessService)
    {
        this.availabilityDao = availabilityDao;
        this.businessHoursDao = businessHoursDao;
        this.salonServiceDao = salonServiceDao;
        this.businessService = businessService;
    }

    /** from and to are dates on the salon's clock, both included; from defaults to today and to to from. */
    public Availability getAvailability(long businessId, long serviceId, LocalDate from, LocalDate to, Long staffId,
            AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        Business business = businessService.getBusinessById((int) businessId);
        ZoneId zone = ZoneId.of(business.getTimezone());
        SalonService service = findActiveService(businessId, serviceId);

        LocalDate first = from != null ? from : LocalDate.now(zone);
        LocalDate last = to != null ? to : first;
        if (last.isBefore(first))
            throw new BaseException("to must not be before from.", ErrorCode.BAD_REQUEST);
        if (ChronoUnit.DAYS.between(first, last) >= MAX_DAYS)
            throw new BaseException("Ask for at most " + MAX_DAYS + " days at a time.", ErrorCode.BAD_REQUEST);

        List<Performer> staff = bookableStaff(businessId, serviceId, staffId);
        List<OpeningInterval> hours = businessHoursDao.getHoursForBusiness(businessId);
        Map<Long, List<WorkingInterval>> shifts = availabilityDao.getShiftsForBusiness(businessId).stream()
                .collect(groupingBy(Shift::staffId, mapping(Shift::interval, toList())));
        OffsetDateTime start = first.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = last.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        Map<Long, List<BusyTime>> busy = availabilityDao.getBusyTimesForBusiness(businessId, start, end).stream()
                .collect(groupingBy(BusyTime::staffId));

        Duration length = Duration.ofMinutes(service.getDuration());
        Instant now = Instant.now();
        List<StaffSlots> slots = staff.stream()
                .map(member -> new StaffSlots(member.id(), member.firstName(), member.lastName(),
                        openSlots(first, last, zone, length, hours, shifts.getOrDefault(member.id(), List.of()),
                                busy.getOrDefault(member.id(), List.of()), now)))
                .toList();
        return new Availability(business.getTimezone(), service.getDuration(), slots);
    }

    private SalonService findActiveService(long businessId, long serviceId)
    {
        SalonService service;
        try {
            service = salonServiceDao.getServiceById((int) businessId, (int) serviceId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Service with id " + serviceId + " not found.", ErrorCode.NOT_FOUND);
        }
        if (!service.isActive())
            throw new BaseException("Service with id " + serviceId + " is not active.", ErrorCode.BAD_REQUEST);
        return service;
    }

    /** Every active performer, or just staffId, which then has to be one, as booking would require. */
    private List<Performer> bookableStaff(long businessId, long serviceId, Long staffId)
    {
        List<Performer> performers = availabilityDao.getStaffPerformingService(businessId, serviceId);
        if (staffId == null)
            return performers.stream().filter(Performer::active).toList();

        Performer member = performers.stream()
                .filter(performer -> performer.id() == staffId)
                .findFirst()
                .orElseThrow(() -> new BaseException(
                        "Staff with id " + staffId + " does not perform service " + serviceId + ".",
                        ErrorCode.BAD_REQUEST));
        if (!member.active())
            throw new BaseException("Staff with id " + staffId + " is not active.", ErrorCode.BAD_REQUEST);
        return List.of(member);
    }

    private static List<Slot> openSlots(LocalDate first, LocalDate last, ZoneId zone, Duration length,
            List<OpeningInterval> hours, List<WorkingInterval> shifts, List<BusyTime> busy, Instant now)
    {
        List<Slot> slots = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            for (Window window : windows(day.getDayOfWeek(), hours, shifts)) {
                Instant windowEnd = day.atTime(window.until()).atZone(zone).toInstant();
                // Start and end are compared as instants, so a slot across a daylight-saving change keeps its length.
                for (LocalDateTime startsAt = day.atStartOfDay().plusMinutes(firstGridMinute(window.from()));
                        ; startsAt = startsAt.plusMinutes(GRID_MINUTES)) {
                    Instant slotStart = startsAt.atZone(zone).toInstant();
                    Instant slotEnd = slotStart.plus(length);
                    if (slotEnd.isAfter(windowEnd))
                        break;
                    if (!slotStart.isBefore(now) && isFree(slotStart, slotEnd, busy))
                        slots.add(new Slot(startsAt, slotEnd.atZone(zone).toLocalDateTime()));
                }
            }
        }
        return slots;
    }

    private record Window(LocalTime from, LocalTime until)
    {
    }

    /** Where the salon is open and the staff member is on shift that weekday, in order, touching ones joined up. */
    private static List<Window> windows(DayOfWeek day, List<OpeningInterval> hours, List<WorkingInterval> shifts)
    {
        // Both lists are ordered by day and start, and a day's intervals never overlap, so neither do these.
        List<Window> windows = new ArrayList<>();
        for (OpeningInterval open : hours) {
            if (open.dayOfWeek() != day)
                continue;
            for (WorkingInterval shift : shifts) {
                if (shift.dayOfWeek() != day)
                    continue;
                LocalTime from = later(open.opensAt(), shift.startsAt());
                LocalTime until = earlier(open.closesAt(), shift.endsAt());
                if (!from.isBefore(until))
                    continue;
                Window previous = windows.isEmpty() ? null : windows.getLast();
                if (previous != null && previous.until().equals(from))
                    windows.set(windows.size() - 1, new Window(previous.from(), until));
                else
                    windows.add(new Window(from, until));
            }
        }
        return windows;
    }

    /** Minutes into the day of the first quarter hour at or after time; 1440 if that is the next midnight. */
    private static int firstGridMinute(LocalTime time)
    {
        int minutes = (time.toSecondOfDay() + 59) / 60;
        return (minutes + GRID_MINUTES - 1) / GRID_MINUTES * GRID_MINUTES;
    }

    private static boolean isFree(Instant from, Instant until, List<BusyTime> busy)
    {
        return busy.stream().noneMatch(time -> time.startsAt().isBefore(until) && from.isBefore(time.endsAt()));
    }

    private static LocalTime later(LocalTime a, LocalTime b)
    {
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime earlier(LocalTime a, LocalTime b)
    {
        return a.isBefore(b) ? a : b;
    }
}
