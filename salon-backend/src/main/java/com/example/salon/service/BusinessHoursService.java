package com.example.salon.service;

import com.example.salon.dao.BusinessHoursDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Business;
import com.example.salon.model.BusinessHours;
import com.example.salon.model.OpeningInterval;
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
 * A salon's weekly opening hours. Anyone who can see the salon can see its hours; only its ADMIN (or a
 * SUPER_ADMIN) can change them, and always as a whole week.
 */
@Service
public class BusinessHoursService
{
    private static final Logger log = LoggerFactory.getLogger(BusinessHoursService.class);

    private static final Comparator<OpeningInterval> BY_DAY_THEN_OPENING =
            Comparator.comparing(OpeningInterval::dayOfWeek).thenComparing(OpeningInterval::opensAt);

    private final BusinessHoursDao businessHoursDao;
    private final BusinessService businessService;

    @Autowired
    public BusinessHoursService(BusinessHoursDao businessHoursDao, BusinessService businessService)
    {
        this.businessHoursDao = businessHoursDao;
        this.businessService = businessService;
    }

    public BusinessHours getHours(long businessId, AuthenticatedUser caller)
    {
        // Same visibility as the salon: a 404 for one the caller isn't allowed to see.
        Business business = businessService.getBusinessById((int) businessId, caller);
        return new BusinessHours(business.getTimezone(), businessHoursDao.getHoursForBusiness(businessId));
    }

    @Transactional
    public BusinessHours replaceHours(long businessId, List<OpeningInterval> hours, AuthenticatedUser caller)
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        Business business = businessService.getBusinessById((int) businessId);

        List<OpeningInterval> week = hours.stream().sorted(BY_DAY_THEN_OPENING).toList();
        requireValidWeek(week);
        businessHoursDao.replaceHoursForBusiness(businessId, week);
        log.info("Replaced opening hours of business {} with {} intervals", businessId, week.size());
        return new BusinessHours(business.getTimezone(), week);
    }

    /** week must be sorted by day, then opening time, so overlaps on a day are neighbours. */
    private static void requireValidWeek(List<OpeningInterval> week)
    {
        OpeningInterval previous = null;
        for (OpeningInterval interval : week) {
            if (!interval.opensAt().isBefore(interval.closesAt()))
                throw new BaseException(interval.dayOfWeek() + " " + interval.opensAt() + "-" + interval.closesAt()
                        + ": closes_at must be after opens_at.", ErrorCode.BAD_REQUEST);
            if (previous != null && previous.dayOfWeek() == interval.dayOfWeek()
                    && interval.opensAt().isBefore(previous.closesAt()))
                throw new BaseException(interval.dayOfWeek() + " " + previous.opensAt() + "-" + previous.closesAt()
                        + " overlaps " + interval.opensAt() + "-" + interval.closesAt() + ".", ErrorCode.BAD_REQUEST);
            previous = interval;
        }
    }
}
