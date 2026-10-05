/*
  =======================
   DB-09: every timestamp is a point in time (timestamptz)
  =======================
 */
-- The plain cast reads each stored value in the session time zone. That is the zone now() wrote it
-- in: both the app and Flyway connect through the JDBC driver, which sets the session zone to the
-- JVM's. So the instants the API has been returning are kept exactly.
ALTER TABLE businesses
    ALTER COLUMN created_at TYPE timestamptz,
    ALTER COLUMN updated_at TYPE timestamptz;
ALTER TABLE users
    ALTER COLUMN created_at TYPE timestamptz,
    ALTER COLUMN updated_at TYPE timestamptz;
ALTER TABLE staff
    ALTER COLUMN created_at TYPE timestamptz,
    ALTER COLUMN updated_at TYPE timestamptz;
ALTER TABLE addresses
    ALTER COLUMN created_at TYPE timestamptz,
    ALTER COLUMN updated_at TYPE timestamptz;
ALTER TABLE contacts
    ALTER COLUMN created_at TYPE timestamptz,
    ALTER COLUMN updated_at TYPE timestamptz;
ALTER TABLE services
    ALTER COLUMN created_at TYPE timestamptz,
    ALTER COLUMN updated_at TYPE timestamptz;
ALTER TABLE customers
    ALTER COLUMN created_at TYPE timestamptz,
    ALTER COLUMN updated_at TYPE timestamptz;

/*
  =======================
   DB-07: coordinates are numbers
  =======================
 */
-- A coordinate that isn't a valid number in range, or is missing its other half, can't be used for
-- anything. Clear the pair rather than fail the migration.
UPDATE addresses SET latitude = NULL, longitude = NULL
WHERE (latitude IS NOT NULL OR longitude IS NOT NULL)
  AND NOT COALESCE(
      CASE
          WHEN latitude ~ '^\s*[-+]?[0-9]{1,3}(\.[0-9]+)?\s*$'
           AND longitude ~ '^\s*[-+]?[0-9]{1,3}(\.[0-9]+)?\s*$'
          THEN trim(latitude)::numeric BETWEEN -90 AND 90
           AND trim(longitude)::numeric BETWEEN -180 AND 180
      END,
      false);

-- NUMERIC(9,6) is about 11 cm of precision, plenty for a salon's front door.
ALTER TABLE addresses
    ALTER COLUMN latitude TYPE NUMERIC(9,6) USING round(trim(latitude)::numeric, 6),
    ALTER COLUMN longitude TYPE NUMERIC(9,6) USING round(trim(longitude)::numeric, 6);

ALTER TABLE addresses
    ADD CONSTRAINT addresses_latitude_range_check CHECK (latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT addresses_longitude_range_check CHECK (longitude BETWEEN -180 AND 180),
    ADD CONSTRAINT addresses_coordinates_pair_check CHECK ((latitude IS NULL) = (longitude IS NULL));

/*
  =======================
   DB-08: money is exact, and says which currency it is in
  =======================
 */
-- services.price is already NUMERIC(10,2); the Java side moves from double to BigDecimal.
-- A negative price predates request validation (1.4) and can't be fixed automatically.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM services WHERE price < 0) THEN
        RAISE EXCEPTION 'V9: some services have a negative price. '
            'Correct them, then restart to re-run this migration.';
    END IF;
END $$;

ALTER TABLE services ADD CONSTRAINT services_price_non_negative_check CHECK (price >= 0);

-- One currency per salon: every price it lists, and later every appointment's price snapshot,
-- is in this ISO 4217 code. Existing salons were all priced in euros.
ALTER TABLE businesses ADD COLUMN currency CHAR(3) NOT NULL DEFAULT 'EUR';
ALTER TABLE businesses ADD CONSTRAINT businesses_currency_format_check CHECK (currency ~ '^[A-Z]{3}$');
