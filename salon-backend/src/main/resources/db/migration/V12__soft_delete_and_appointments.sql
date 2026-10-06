/*
  =======================
   DB-13: removing a service, staff member or customer keeps the row
  =======================
 */
-- Appointments keep pointing at the service, staff member and customer they were for, so the API's DELETE
-- now stamps deleted_at instead and every read leaves those rows out. A salon itself is still deleted
-- outright, and takes all of them with it.
ALTER TABLE services ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE staff ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE customers ADD COLUMN deleted_at TIMESTAMPTZ;

-- Someone taken off the staff can be taken on again later, as a new staff record. V1 and V2 each made
-- user_id UNIQUE; one partial index replaces both, and a plain one serves fk_staff_user (DB-12).
ALTER TABLE staff DROP CONSTRAINT staff_user_id_key, DROP CONSTRAINT uq_staff_user;
CREATE UNIQUE INDEX staff_user_unique_idx ON staff (user_id) WHERE deleted_at IS NULL;
CREATE INDEX staff_user_id_idx ON staff (user_id);

-- Likewise an account can be linked again by a salon that removed its customer record.
DROP INDEX customers_business_user_unique_idx;
CREATE UNIQUE INDEX customers_business_user_unique_idx ON customers (business_id, user_id)
    WHERE user_id IS NOT NULL AND deleted_at IS NULL;

/*
  =======================
   DB-14: appointments
  =======================
 */
-- One service by one staff member for one customer; a visit with several services is several appointments
-- back to back. business_id rides along on every foreign key, as on staff_services (V11), so an appointment
-- can't pick up another salon's customer, staff member or service.
ALTER TABLE customers ADD CONSTRAINT customers_id_business_unique UNIQUE (id, business_id);

CREATE TABLE appointments (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    -- What the service cost when it was booked, in businesses.currency; later price changes leave it alone.
    price NUMERIC(10,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,

    -- A deleted salon takes its appointments with it. Customers, staff and services are soft-deleted (DB-13),
    -- and these plain foreign keys refuse to delete one outright while an appointment points at it.
    CONSTRAINT fk_appointments_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointments_customer FOREIGN KEY (customer_id, business_id) REFERENCES customers(id, business_id),
    CONSTRAINT fk_appointments_staff FOREIGN KEY (staff_id, business_id) REFERENCES staff(id, business_id),
    CONSTRAINT fk_appointments_service FOREIGN KEY (service_id, business_id) REFERENCES services(id, business_id),
    CONSTRAINT appointments_order_check CHECK (starts_at < ends_at),
    CONSTRAINT appointments_status_check
        CHECK (status IN ('BOOKED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT appointments_price_non_negative_check CHECK (price >= 0),
    -- The database itself refuses to double-book a staff member. Back to back is fine, and a cancelled or
    -- missed appointment frees its time, e.g. for a walk-in.
    CONSTRAINT appointments_no_double_booking EXCLUDE USING gist (
        staff_id WITH =, tstzrange(starts_at, ends_at) WITH &&) WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'))
);

-- A salon's day, a staff member's calendar and a customer's history are each read by time. Their leading
-- columns also serve the foreign keys (DB-12), which the partial exclusion index can't.
CREATE INDEX appointments_business_starts_at_idx ON appointments (business_id, starts_at);
CREATE INDEX appointments_staff_starts_at_idx ON appointments (staff_id, starts_at);
CREATE INDEX appointments_customer_starts_at_idx ON appointments (customer_id, starts_at);
CREATE INDEX appointments_service_id_idx ON appointments (service_id);

CREATE TRIGGER appointments_set_updated_at BEFORE UPDATE ON appointments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
