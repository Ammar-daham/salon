package com.example.salon.dao;

import com.example.salon.model.TimeOff;

import java.time.ZoneId;
import java.util.List;

/** zone is the salon's time zone: TimeOff's date-times are on its clock, the table stores instants. */
public interface StaffTimeOffDao {
    Long addTimeOff(long staffId, TimeOff timeOff, ZoneId zone);

    List<TimeOff> getTimeOffForStaff(long staffId, ZoneId zone);

    TimeOff getTimeOffById(long staffId, long timeOffId, ZoneId zone);

    int updateTimeOff(long staffId, long timeOffId, TimeOff timeOff, ZoneId zone);

    int deleteTimeOff(long staffId, long timeOffId);
}
