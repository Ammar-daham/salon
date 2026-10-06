package com.example.salon.service;

import com.example.salon.dao.AppointmentDao;
import com.example.salon.dao.SalonServiceDao;
import com.example.salon.dao.StaffDao;
import com.example.salon.dao.UserDao;
import com.example.salon.exception.BaseException;
import com.example.salon.exception.ErrorCode;
import com.example.salon.model.Role;
import com.example.salon.model.SalonService;
import com.example.salon.model.Staff;
import com.example.salon.model.User;
import com.example.salon.security.AccessControl;
import com.example.salon.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StaffService
{
    private static final Logger log = LoggerFactory.getLogger(StaffService.class);

    private final StaffDao staffDao;
    private final UserDao userDao;
    private final SalonServiceDao salonServiceDao;
    private final AppointmentDao appointmentDao;
    private final BusinessService businessService;

    @Autowired
    public StaffService(StaffDao staffDao, UserDao userDao, SalonServiceDao salonServiceDao,
            AppointmentDao appointmentDao, BusinessService businessService)
    {
        this.staffDao = staffDao;
        this.userDao = userDao;
        this.salonServiceDao = salonServiceDao;
        this.appointmentDao = appointmentDao;
        this.businessService = businessService;
    }

    @Transactional
    public Staff addStaffForBusiness(Long businessId, Staff staff, AuthenticatedUser caller)
    {
        AccessControl.requireBusinessAccess(caller, businessId);

        User user = userDao.findById(staff.getUserId())
                .orElseThrow(() -> new BaseException(
                        "User with id " + staff.getUserId() + " not found", ErrorCode.NOT_FOUND));
        if (!businessId.equals(user.getBusinessId())) {
            throw new BaseException(
                    "User with id " + staff.getUserId() + " does not belong to this business",
                    ErrorCode.BAD_REQUEST);
        }
        if (user.getRole() != Role.EMPLOYEE && user.getRole() != Role.ADMIN) {
            throw new BaseException(
                    "Only an EMPLOYEE or ADMIN can have a staff record", ErrorCode.BAD_REQUEST);
        }

        staff.setBusinessId(businessId);
        try {
            Long id = staffDao.addStaff(staff);
            staff.setId(id);
        } catch (DuplicateKeyException ex) {
            throw new BaseException("This user already has a staff record.", ErrorCode.DUPLICATE_RESOURCE);
        }
        log.info("Added user {} to the staff of business {} as staff {}", staff.getUserId(), businessId, staff.getId());
        return staff;
    }

    public List<Staff> getStaffForBusiness(Long businessId)
    {
        return staffDao.getStaffForBusiness(businessId);
    }

    public Staff getStaffById(long businessId, long staffId)
    {
        try {
            return staffDao.getStaffById(businessId, staffId);
        } catch (EmptyResultDataAccessException ex) {
            throw new BaseException("Staff with id " + staffId + " not found.", ErrorCode.NOT_FOUND);
        }
    }

    @Transactional
    public void updateStaffById(long businessId, long staffId, Staff staff, AuthenticatedUser caller)
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        getStaffById(businessId, staffId);
        int row = staffDao.updateStaffById(staffId, staff);
        if (row == 0)
            throw new BaseException("Staff with id " + staffId + " not found.", ErrorCode.NOT_FOUND);
        log.info("Updated staff {} in business {}", staffId, businessId);
    }

    @Transactional
    public void deleteStaffById(long businessId, long staffId, AuthenticatedUser caller)
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        getStaffById(businessId, staffId);
        if (appointmentDao.hasUpcomingAppointmentsForStaff(staffId))
            throw new BaseException("Staff with id " + staffId + " still has upcoming appointments. "
                    + "Cancel or move them first.", ErrorCode.DUPLICATE_RESOURCE);
        int row = staffDao.deleteStaffById(staffId);
        if (row == 0)
            throw new BaseException("Staff with id " + staffId + " not found.", ErrorCode.NOT_FOUND);
        log.info("Removed staff {} from business {}", staffId, businessId);
    }

    /** The services a staff member performs: public like the salon's own list, so a 404 for a salon you can't see. */
    public List<SalonService> getServicesOfStaff(long businessId, long staffId, AuthenticatedUser caller)
    {
        businessService.getBusinessById((int) businessId, caller);
        getStaffById(businessId, staffId);
        return salonServiceDao.getServicesForStaff(staffId);
    }

    @Transactional
    public List<SalonService> replaceServicesOfStaff(long businessId, long staffId, List<Long> serviceIds,
            AuthenticatedUser caller)
    {
        AccessControl.requireBusinessAccess(caller, businessId);
        getStaffById(businessId, staffId);

        Set<Long> offered = salonServiceDao.getServicesForBusiness(businessId).stream()
                .map(SalonService::getId)
                .collect(Collectors.toSet());
        Set<Long> performed = new LinkedHashSet<>(serviceIds);
        for (Long serviceId : performed) {
            if (!offered.contains(serviceId))
                throw new BaseException("Service with id " + serviceId + " is not offered by this business.",
                        ErrorCode.BAD_REQUEST);
        }

        staffDao.replaceServicesOfStaff(businessId, staffId, performed);
        log.info("Staff {} in business {} now performs {} services", staffId, businessId, performed.size());
        return salonServiceDao.getServicesForStaff(staffId);
    }
}
