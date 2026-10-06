package com.example.salon.dao;

import com.example.salon.model.Staff;

import java.util.Collection;
import java.util.List;

public interface StaffDao {
    Long addStaff(Staff staff);

    List<Staff> getStaffForBusiness(Long businessId);

    Staff getStaffById(long businessId, long staffId);

    int updateStaffById(long id, Staff staff);

    int deleteStaffById(long id);

    /** Call inside a transaction: the old set is deleted before the new one is written. */
    void replaceServicesOfStaff(long businessId, long staffId, Collection<Long> serviceIds);
}
