package com.example.salon.dao;

import com.example.salon.model.Staff;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class StaffDataAccessService implements StaffDao {
    private final JdbcTemplate jdbcTemplate;

    public StaffDataAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long addUser(Staff staff) {
        return 0;
    }

    @Override
    public List<Staff> getAllStaff() {
        return List.of();
    }

    @Override
    public Staff getStaffById(int id) {
        return null;
    }

    @Override
    public long updateStaffById(long id, Staff staff) {
        return 0;
    }

    @Override
    public long deleteStaffById(long id, Staff staff) {
        return 0;
    }
}
