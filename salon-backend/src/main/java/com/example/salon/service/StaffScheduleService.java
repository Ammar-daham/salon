package com.example.salon.service;

import com.example.salon.dao.StaffScheduleDao;
import com.example.salon.dao.StaffTimeOffDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Business;
import com.example.salon.model.Staff;
import com.example.salon.model.StaffSchedule;
import com.example.salon.model.TimeOff;
import com.example.salon.model.WorkingInterval;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/**
 * When a staff member works: their weekly hours and their time off. Only the salon's ADMIN (or a
 * SUPER_ADMIN) changes either. Anyone on the salon's staff can see the weekly hours, since the team plans
 * around them; time off may say why someone is away, so only an admin and the person themselves see it.
 */
@Service
public class StaffScheduleService
{
    private static final Logger log = LoggerFactory.getLogger(StaffScheduleService.class);

    private static final Comparator<WorkingInterval> BY_DAY_THEN_START =
            Comparator.comparing(WorkingInterval::dayOfWeek).thenComparing(WorkingInterval::startsAt);

    private final StaffScheduleDao staffScheduleDao;
    private final StaffTimeOffDao staffTimeOffDao;
    private final StaffService staffService;
    private final BusinessService businessService;

    @Autowired
    public StaffScheduleService(StaffScheduleDao staffScheduleDao, StaffTimeOffDao staffTimeOffDao,
            StaffService staffService, BusinessService businessService)
    {
        this.staffScheduleDao = staffScheduleDao;
        this.staffTimeOffDao = staffTimeOffDao;
        this.staffService = staffService;
        this.businessService = businessService;
    }

    public StaffSchedule getSchedule(long businessId, long staffId, AuthenticatedUser caller)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        Business business = businessService.getBusinessById((int) businessId);
        staffService.getStaffById(businessId, staffId);
        return new StaffSchedule(business.getTimezone(), staffScheduleDao.getScheduleForStaff(staffId));
    }

    @Transactional
    public StaffSchedule replaceSchedule(long businessId, long staffId, List<WorkingInterval> hours,
            AuthenticatedUser caller)
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        Business business = businessService.getBusinessById((int) businessId);
        staffService.getStaffById(businessId, staffId);

        List<WorkingInterval> week = hours.stream().sorted(BY_DAY_THEN_START).toList();
        requireValidWeek(week);
        staffScheduleDao.replaceScheduleForStaff(staffId, week);
        log.info("Replaced the weekly hours of staff {} in business {} with {} intervals", staffId, businessId,
                week.size());
        return new StaffSchedule(business.getTimezone(), week);
    }

    public List<TimeOff> getTimeOff(long businessId, long staffId, AuthenticatedUser caller)
    {
        ZoneId zone = requireTimeOffReader(caller, businessId, staffId);
        return staffTimeOffDao.getTimeOffForStaff(staffId, zone);
    }

    public TimeOff getTimeOffById(long businessId, long staffId, long timeOffId, AuthenticatedUser caller)
    {
        ZoneId zone = requireTimeOffReader(caller, businessId, staffId);
        return findTimeOff(staffId, timeOffId, zone);
    }

    @Transactional
    public TimeOff addTimeOff(long businessId, long staffId, TimeOff timeOff, AuthenticatedUser caller)
    {
        ZoneId zone = requireTimeOffWriter(caller, businessId, staffId, timeOff);
        Long id;
        try {
            id = staffTimeOffDao.addTimeOff(staffId, timeOff, zone);
        } catch (DataIntegrityViolationException ex) {
            throw overlapOrRethrow(ex);
        }
        log.info("Added time off {} for staff {} in business {}", id, staffId, businessId);
        return findTimeOff(staffId, id, zone);
    }

    @Transactional
    public void updateTimeOff(long businessId, long staffId, long timeOffId, TimeOff timeOff, AuthenticatedUser caller)
    {
        ZoneId zone = requireTimeOffWriter(caller, businessId, staffId, timeOff);
        int row;
        try {
            row = staffTimeOffDao.updateTimeOff(staffId, timeOffId, timeOff, zone);
        } catch (DataIntegrityViolationException ex) {
            throw overlapOrRethrow(ex);
        }
        if (row == 0)
            throw new BaseException("Time off with id " + timeOffId + " not found.", ErrorCode.NOT_FOUND);
        log.info("Updated time off {} for staff {} in business {}", timeOffId, staffId, businessId);
    }

    @Transactional
    public void deleteTimeOff(long businessId, long staffId, long timeOffId, AuthenticatedUser caller)
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        staffService.getStaffById(businessId, staffId);
        if (staffTimeOffDao.deleteTimeOff(staffId, timeOffId) == 0)
            throw new BaseException("Time off with id " + timeOffId + " not found.", ErrorCode.NOT_FOUND);
        log.info("Deleted time off {} for staff {} in business {}", timeOffId, staffId, businessId);
    }

    /** Returns the salon's zone once the caller may read this staff member's time off. */
    private ZoneId requireTimeOffReader(AuthenticatedUser caller, long businessId, long staffId)
    {
        AccessControl.requireStaffOfBusiness(caller, businessId);
        Business business = businessService.getBusinessById((int) businessId);
        Staff staff = staffService.getStaffById(businessId, staffId);
        if (!AccessControl.isAdmin(caller) && !AccessControl.isSelf(caller, staff.getUserId()))
            throw new AccessDeniedException("Only an admin or the staff member can see their time off");
        return ZoneId.of(business.getTimezone());
    }

    /** Returns the salon's zone once the caller may write this time off and it ends after it starts. */
    private ZoneId requireTimeOffWriter(AuthenticatedUser caller, long businessId, long staffId, TimeOff timeOff)
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        Business business = businessService.getBusinessById((int) businessId);
        staffService.getStaffById(businessId, staffId);
        if (!timeOff.endsAt().isAfter(timeOff.startsAt()))
            throw new BaseException("ends_at must be after starts_at.", ErrorCode.BAD_REQUEST);
        return ZoneId.of(business.getTimezone());
    }

    private TimeOff findTimeOff(long staffId, long timeOffId, ZoneId zone)
    {
        try {
            return staffTimeOffDao.getTimeOffById(staffId, timeOffId, zone);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Time off with id " + timeOffId + " not found.", ErrorCode.NOT_FOUND);
        }
    }

    private static RuntimeException overlapOrRethrow(DataIntegrityViolationException ex)
    {
        if (ex.getMessage() == null || !ex.getMessage().contains("staff_time_off_no_overlap"))
            return ex;
        return new BaseException("This time off overlaps another absence of the same staff member.",
                ErrorCode.DUPLICATE_RESOURCE);
    }

    /** week must be sorted by day, then start, so overlaps on a day are neighbours. */
    private static void requireValidWeek(List<WorkingInterval> week)
    {
        WorkingInterval previous = null;
        for (WorkingInterval interval : week) {
            if (!interval.startsAt().isBefore(interval.endsAt()))
                throw new BaseException(interval.dayOfWeek() + " " + interval.startsAt() + "-" + interval.endsAt()
                        + ": ends_at must be after starts_at.", ErrorCode.BAD_REQUEST);
            if (previous != null && previous.dayOfWeek() == interval.dayOfWeek()
                    && interval.startsAt().isBefore(previous.endsAt()))
                throw new BaseException(interval.dayOfWeek() + " " + previous.startsAt() + "-" + previous.endsAt()
                        + " overlaps " + interval.startsAt() + "-" + interval.endsAt() + ".", ErrorCode.BAD_REQUEST);
            previous = interval;
        }
    }
}
