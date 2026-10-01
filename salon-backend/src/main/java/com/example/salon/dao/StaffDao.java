package com.example.salon.dao;

import com.example.salon.model.Staff;

import java.util.List;

public interface StaffDao {
    Long addStaff(Staff staff);

    List<Staff> getStaffForBusiness(Long businessId);

    Staff getStaffById(long businessId, long staffId);

    int updateStaffById(long id, Staff staff);

    int deleteStaffById(long id);
}
