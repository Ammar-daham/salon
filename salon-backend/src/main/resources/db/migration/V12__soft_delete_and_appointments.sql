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
