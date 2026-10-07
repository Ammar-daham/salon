package com.example.salon.dao;

import com.example.salon.model.WorkingInterval;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/** What availability reads, each for a whole salon at once rather than once per staff member. */
public interface AvailabilityDao {
    /** A current staff member who performs the service, active or not, with the name to show. */
    record Performer(long id, String firstName, String lastName, boolean active)
    {
    }

    /** One row of staff_schedules. */
    record Shift(long staffId, WorkingInterval interval)
    {
    }

    /** Time a staff member can't be booked: time off, or an appointment that hasn't been cancelled or missed. */
    record BusyTime(long staffId, Instant startsAt, Instant endsAt)
    {
    }

    /** By name. */
    List<Performer> getStaffPerformingService(long businessId, long serviceId);

    /** Of current staff members. */
    List<Shift> getShiftsForBusiness(long businessId);

    /** Everything that overlaps [from, to). */
    List<BusyTime> getBusyTimesForBusiness(long businessId, OffsetDateTime from, OffsetDateTime to);
}
