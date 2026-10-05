package com.example.salon.model;

import java.util.List;

/** A staff member's weekly working hours, ordered by day and then start, with the salon's time zone they are in. */
public record StaffSchedule(String timezone, List<WorkingInterval> hours)
{
}
