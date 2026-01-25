package com.example.salon.dao;

import com.example.salon.model.Staff;
import com.example.salon.model.User;

import java.util.List;

public interface StaffDao {
    long addUser(Staff staff);

    List<Staff> getAllStaff();

    Staff getStaffById(int id);

    long updateStaffById(long id, Staff staff);

    long deleteStaffById(long id, Staff staff);
}
