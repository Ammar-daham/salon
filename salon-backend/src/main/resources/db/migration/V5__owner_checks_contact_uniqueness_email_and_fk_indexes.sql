/*
  =======================
   DB-06: every address and contact has exactly one owner
  =======================
 */
-- Rows with no owner are left over from deletes before BE-10 (the old FKs were ON DELETE SET NULL).
-- Nothing can reach them, and an orphaned contact still holds its value under the old global UNIQUE.
DELETE FROM addresses WHERE business_id IS NULL AND user_id IS NULL;
DELETE FROM contacts WHERE business_id IS NULL AND user_id IS NULL;

-- A row with both owners can't be fixed automatically: we can't tell which owner is right.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM addresses WHERE business_id IS NOT NULL AND user_id IS NOT NULL)
       OR EXISTS (SELECT 1 FROM contacts WHERE business_id IS NOT NULL AND user_id IS NOT NULL) THEN
        RAISE EXCEPTION 'V5: some addresses/contacts have both business_id and user_id set. '
            'Clear one of them on each row, then restart to re-run this migration.';
    END IF;
END $$;

ALTER TABLE addresses
ADD CONSTRAINT addresses_one_owner_check
CHECK (num_nonnulls(business_id, user_id) = 1);

ALTER TABLE contacts
ADD CONSTRAINT contacts_one_owner_check
CHECK (num_nonnulls(business_id, user_id) = 1);

-- ON DELETE SET NULL would now violate the CHECK. A child goes with its owner instead.
ALTER TABLE addresses DROP CONSTRAINT fk_addresses_business;
ALTER TABLE addresses DROP CONSTRAINT fk_addresses_user;
ALTER TABLE contacts DROP CONSTRAINT fk_contacts_business;
ALTER TABLE contacts DROP CONSTRAINT fk_contacts_user;

ALTER TABLE addresses
ADD CONSTRAINT fk_addresses_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE contacts
ADD CONSTRAINT fk_contacts_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_contacts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

/*
  =======================
   DB-05: a contact value is unique per owner, not platform-wide
  =======================
 */
-- Two customers can share a family phone, and a salon can list its owner's email.
ALTER TABLE contacts DROP CONSTRAINT contacts_value_key;

CREATE UNIQUE INDEX contacts_business_value_unique_idx ON contacts (business_id, value)
    WHERE business_id IS NOT NULL;
CREATE UNIQUE INDEX contacts_user_value_unique_idx ON contacts (user_id, value)
    WHERE user_id IS NOT NULL;

/*
  =======================
   DB-10: emails are unique regardless of case
  =======================
 */
DO $$
BEGIN
    IF EXISTS (SELECT lower(email) FROM users WHERE email IS NOT NULL GROUP BY lower(email) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'V5: some users share an email that differs only by case. '
            'Change or remove the duplicates, then restart to re-run this migration.';
    END IF;
END $$;

DROP INDEX users_email_unique_idx;
CREATE UNIQUE INDEX users_email_lower_unique_idx ON users (lower(email)) WHERE email IS NOT NULL;

/*
  =======================
   DB-12: index the foreign-key columns
  =======================
 */
-- Already covered: contacts.business_id and contacts.user_id (the DB-05 indexes lead with them),
-- staff.user_id (UNIQUE) and business_service.business_id (leading column of its primary key).
-- business_customers has no FKs and is replaced by DB-03.
CREATE INDEX users_business_id_idx ON users (business_id);
CREATE INDEX staff_business_id_idx ON staff (business_id);
CREATE INDEX addresses_business_id_idx ON addresses (business_id);
CREATE INDEX addresses_user_id_idx ON addresses (user_id);
CREATE INDEX business_service_service_id_idx ON business_service (service_id);
