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
