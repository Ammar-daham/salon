/*
  =======================
   BE-16 / DB-02: a service belongs to exactly one business
  =======================
 */
-- business_service was many-to-many, but the app only ever linked a service to the one
-- business that created it. A service linked to zero or multiple businesses can't be
-- migrated automatically - it needs a human to decide which business owns it.
DO $$
BEGIN
    IF EXISTS (
        SELECT service_id FROM business_service GROUP BY service_id HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'V6: some services are linked to more than one business. '
            'Resolve which business owns each one, then restart to re-run this migration.';
    END IF;

    IF EXISTS (
        SELECT id FROM services s
        WHERE NOT EXISTS (SELECT 1 FROM business_service bs WHERE bs.service_id = s.id)
    ) THEN
        RAISE EXCEPTION 'V6: some services are not linked to any business. '
            'Link or delete them, then restart to re-run this migration.';
    END IF;
END $$;

ALTER TABLE services ADD COLUMN business_id BIGINT;

UPDATE services s
SET business_id = bs.business_id
FROM business_service bs
WHERE bs.service_id = s.id;

ALTER TABLE services ALTER COLUMN business_id SET NOT NULL;

-- A service is owned by its business; deleting the business deletes the service with it,
-- instead of the old code hard-deleting rows out of what was modeled as a shared table.
ALTER TABLE services
ADD CONSTRAINT fk_services_business
FOREIGN KEY (business_id)
REFERENCES businesses(id)
ON DELETE CASCADE;

CREATE INDEX services_business_id_idx ON services (business_id);

DROP TABLE business_service;
