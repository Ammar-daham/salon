package com.example.salon.service;

import com.example.salon.dao.StaffScheduleDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Business;
import com.example.salon.model.StaffSchedule;
import com.example.salon.model.WorkingInterval;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * When a staff member works: their weekly hours. Only the salon's ADMIN (or a SUPER_ADMIN) changes them;
 * anyone on the salon's staff can see them, since the team plans around them.
 */
@Service
public class StaffScheduleService
{
    private static final Logger log = LoggerFactory.getLogger(StaffScheduleService.class);

    private static final Comparator<WorkingInterval> BY_DAY_THEN_START =
            Comparator.comparing(WorkingInterval::dayOfWeek).thenComparing(WorkingInterval::startsAt);

    private final StaffScheduleDao staffScheduleDao;
    private final StaffService staffService;
    private final BusinessService businessService;

    @Autowired
    public StaffScheduleService(StaffScheduleDao staffScheduleDao, StaffService staffService,
            BusinessService businessService)
    {
        this.staffScheduleDao = staffScheduleDao;
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
