/*
  =======================
   DB-09: a salon keeps time in its own time zone
  =======================
 */
-- Opening hours (and later appointments) are wall-clock times at the salon. Turning them into
-- instants takes the salon's IANA zone, e.g. Europe/Berlin. Every existing salon is in Germany.
ALTER TABLE businesses ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'Europe/Berlin';

/*
  =======================
   DB-14: weekly opening hours
  =======================
 */
-- Lets a GiST exclusion constraint compare plain columns with "=" (needed again for appointments).
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Postgres has range types over timestamps and dates, but none over a time of day.
CREATE TYPE timerange AS RANGE (subtype = time);

-- One row per opening interval. A day with no rows is closed; two rows are e.g. a lunch break.
-- day_of_week is ISO 8601 (1 = Monday ... 7 = Sunday), like extract(isodow ...) and java.time.DayOfWeek.
-- Times are wall-clock in businesses.timezone, and an interval can't run past midnight.
CREATE TABLE business_hours (
    business_id BIGINT NOT NULL,
    day_of_week SMALLINT NOT NULL,
    opens_at TIME NOT NULL,
    closes_at TIME NOT NULL,

    PRIMARY KEY (business_id, day_of_week, opens_at),
    CONSTRAINT fk_business_hours_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT business_hours_day_of_week_check CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT business_hours_order_check CHECK (opens_at < closes_at),
    -- Two intervals on the same day may touch (12:00-13:00, 13:00-17:00) but not overlap.
    CONSTRAINT business_hours_no_overlap EXCLUDE USING gist (
        business_id WITH =, day_of_week WITH =, timerange(opens_at, closes_at) WITH &&)
);
