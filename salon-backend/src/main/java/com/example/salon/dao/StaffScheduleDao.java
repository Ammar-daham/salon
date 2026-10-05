package com.example.salon.dao;

import com.example.salon.model.WorkingInterval;

import java.util.List;

public interface StaffScheduleDao {
    List<WorkingInterval> getScheduleForStaff(long staffId);

    /** Call inside a transaction: the old week is deleted before the new one is written. */
    void replaceScheduleForStaff(long staffId, List<WorkingInterval> hours);
}
