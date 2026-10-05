/*
  =======================
   DB-14: the services each staff member performs
  =======================
 */
-- business_id rides along on both foreign keys, so the database itself refuses to link a staff member
-- to another salon's service. The UNIQUE constraints are what those composite keys point at.
ALTER TABLE staff ADD CONSTRAINT staff_id_business_unique UNIQUE (id, business_id);
ALTER TABLE services ADD CONSTRAINT services_id_business_unique UNIQUE (id, business_id);

CREATE TABLE staff_services (
    business_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,

    PRIMARY KEY (staff_id, service_id),
    CONSTRAINT fk_staff_services_staff FOREIGN KEY (staff_id, business_id)
        REFERENCES staff(id, business_id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_services_service FOREIGN KEY (service_id, business_id)
        REFERENCES services(id, business_id) ON DELETE CASCADE
);

CREATE INDEX staff_services_service_id_idx ON staff_services (service_id, business_id);

/*
  =======================
   DB-14: each staff member's weekly working hours
  =======================
 */
-- The same shape as business_hours (V10): ISO weekday, wall-clock times in businesses.timezone, several
-- shifts a day, none past midnight. Working outside opening hours isn't refused here; availability only
-- offers times inside both.
CREATE TABLE staff_schedules (
    staff_id BIGINT NOT NULL,
    day_of_week SMALLINT NOT NULL,
    starts_at TIME NOT NULL,
    ends_at TIME NOT NULL,

    PRIMARY KEY (staff_id, day_of_week, starts_at),
    CONSTRAINT fk_staff_schedules_staff FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE,
    CONSTRAINT staff_schedules_day_of_week_check CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT staff_schedules_order_check CHECK (starts_at < ends_at),
    CONSTRAINT staff_schedules_no_overlap EXCLUDE USING gist (
        staff_id WITH =, day_of_week WITH =, timerange(starts_at, ends_at) WITH &&)
);
