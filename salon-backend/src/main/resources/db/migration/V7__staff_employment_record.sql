/*
  =======================
   BE-08 / BE-35 / DB-04: staff becomes a real, usable employment record
  =======================
 */
-- Existing staff rows predate hired_at; backfill from when the row itself was created, since
-- that's the closest fact the database actually has.
ALTER TABLE staff ADD COLUMN hired_at DATE;
UPDATE staff SET hired_at = created_at::date WHERE hired_at IS NULL;
ALTER TABLE staff
    ALTER COLUMN hired_at SET NOT NULL,
    ALTER COLUMN hired_at SET DEFAULT CURRENT_DATE;

-- A hex colour the future calendar view (Phase 4) can colour-code this person's appointments
-- with. Nothing sets it yet, so unlike hired_at it stays nullable rather than backfilled.
ALTER TABLE staff ADD COLUMN calendar_colour VARCHAR(7);
ALTER TABLE staff ADD CONSTRAINT staff_calendar_colour_format_check
    CHECK (calendar_colour IS NULL OR calendar_colour ~ '^#[0-9A-Fa-f]{6}$');
