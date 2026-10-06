-- =============================================================================
-- Sample data seed script
-- =============================================================================
--
-- Populates a freshly-migrated database with realistic sample data across every
-- role and resource: a platform super admin, three businesses each with an
-- address, contacts, a handful of services, an admin, two employees (plus a
-- matching `staff` row), and a few customers per business in `customers`.
--
-- This is a standalone script, NOT a Flyway migration - it is not picked up by
-- FlywayConfig (which only scans classpath:db/migration) and will never run
-- automatically. Run it by hand whenever you want fresh sample data:
--
--   psql -h localhost -U postgres -d salondb -f src/main/resources/db/seed_data.sql
--
-- It expects an EMPTY database (just the schema from V1-V3). Running it twice
-- will fail on unique constraints (business names, contact values, user
-- emails). To reset first, uncomment the TRUNCATE block below.
--
-- Every login-capable user (SUPER_ADMIN, ADMIN, EMPLOYEE) shares one password
-- for convenience:
--
--   Password123!
--
-- Customers are rows in `customers`, not users, so they have no login.
-- =============================================================================

-- TRUNCATE staff_time_off, staff_schedules, staff_services, business_hours,
--     customers, staff, services, contacts, addresses, users, businesses RESTART IDENTITY CASCADE;

BEGIN;

DO $$
DECLARE
    pw_hash          TEXT := '$2b$10$zxJ0N4amu7cL1Zx565YfJ.oWgQmgVcfy51f3tzVc0dimXWluIOxhm';

    glow_id          BIGINT;
    urban_id         BIGINT;
    serenity_id      BIGINT;

    glow_admin_id    BIGINT;
    glow_emp1_id     BIGINT;
    glow_emp2_id     BIGINT;

    urban_admin_id   BIGINT;
    urban_emp1_id    BIGINT;
    urban_emp2_id    BIGINT;

    serenity_admin_id BIGINT;
    serenity_emp1_id  BIGINT;
    serenity_emp2_id  BIGINT;

BEGIN

    -- =========================================================================
    -- Super admin - platform-wide, not tied to any business.
    -- =========================================================================
    INSERT INTO users (first_name, last_name, role, email, password_hash)
    VALUES ('Sam', 'Root', 'SUPER_ADMIN', 'superadmin@salon.example.com', pw_hash);

    -- =========================================================================
    -- Business 1: Glow Beauty Studio (APPROVED)
    -- =========================================================================
    INSERT INTO businesses (name, description, image, status)
    VALUES ('Glow Beauty Studio',
            'A calm, modern salon specialising in colour and precision cuts.',
            'https://picsum.photos/seed/glow-beauty-studio/800/500',
            'APPROVED')
    RETURNING id INTO glow_id;

    INSERT INTO addresses (street, city, country, postal_code, business_id)
    VALUES ('12 Rosenthaler Str.', 'Berlin', 'Germany', '10119', glow_id);

    INSERT INTO contacts (type, value, business_id)
    VALUES ('phone', '+49 30 1234501', glow_id),
           ('email', 'hello@glowbeauty.example.com', glow_id);

    INSERT INTO services (business_id, name, description, duration_minutes, price, is_active)
    VALUES (glow_id, 'Signature Haircut', 'Wash, cut and style.', 45, 45.00, true);

    INSERT INTO services (business_id, name, description, duration_minutes, price, is_active)
    VALUES (glow_id, 'Balayage Colour', 'Hand-painted colour with gloss finish.', 120, 150.00, true);

    INSERT INTO services (business_id, name, description, duration_minutes, price, is_active)
    VALUES (glow_id, 'Classic Manicure', 'Shape, cuticle care and polish.', 30, 25.00, true);

    -- Tuesday to Friday 09:00-18:00, Saturday 09:00-14:00.
    INSERT INTO business_hours (business_id, day_of_week, opens_at, closes_at)
    VALUES (glow_id, 2, '09:00', '18:00'), (glow_id, 3, '09:00', '18:00'), (glow_id, 4, '09:00', '18:00'),
           (glow_id, 5, '09:00', '18:00'), (glow_id, 6, '09:00', '14:00');

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Anna', 'Admin', 'ADMIN', glow_id, 'anna.admin@glowbeauty.example.com', pw_hash)
    RETURNING id INTO glow_admin_id;

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Mia', 'Stylist', 'EMPLOYEE', glow_id, 'mia.stylist@glowbeauty.example.com', pw_hash)
    RETURNING id INTO glow_emp1_id;
    INSERT INTO staff (user_id, business_id, title, is_active, hired_at) VALUES (glow_emp1_id, glow_id, 'Senior Stylist', true, '2021-03-15');

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Noah', 'Colorist', 'EMPLOYEE', glow_id, 'noah.colorist@glowbeauty.example.com', pw_hash)
    RETURNING id INTO glow_emp2_id;
    INSERT INTO staff (user_id, business_id, title, is_active, hired_at) VALUES (glow_emp2_id, glow_id, 'Colorist', true, '2022-07-01');

    -- =========================================================================
    -- Business 2: Urban Cuts Barbershop (APPROVED)
    -- =========================================================================
    INSERT INTO businesses (name, description, image, status)
    VALUES ('Urban Cuts Barbershop',
            'No-frills fades, beard work and hot towel shaves.',
            'https://picsum.photos/seed/urban-cuts-barbershop/800/500',
            'APPROVED')
    RETURNING id INTO urban_id;

    INSERT INTO addresses (street, city, country, postal_code, business_id)
    VALUES ('45 Kastanienallee', 'Berlin', 'Germany', '10435', urban_id);

    INSERT INTO contacts (type, value, business_id)
    VALUES ('phone', '+49 30 1234502', urban_id),
           ('email', 'hello@urbancuts.example.com', urban_id);

    INSERT INTO services (business_id, name, description, duration_minutes, price, is_active)
    VALUES (urban_id, 'Classic Fade', 'Skin fade with a straight-razor finish.', 30, 28.00, true);

    INSERT INTO services (business_id, name, description, duration_minutes, price, is_active)
    VALUES (urban_id, 'Beard Trim', 'Shape and line-up.', 20, 15.00, true);

    INSERT INTO services (business_id, name, description, duration_minutes, price, is_active)
    VALUES (urban_id, 'Hot Towel Shave', 'Traditional straight-razor shave.', 40, 35.00, true);

    -- Monday to Friday with a lunch break, Saturday 10:00-16:00.
    INSERT INTO business_hours (business_id, day_of_week, opens_at, closes_at)
    VALUES (urban_id, 1, '10:00', '13:00'), (urban_id, 1, '14:00', '20:00'),
           (urban_id, 2, '10:00', '13:00'), (urban_id, 2, '14:00', '20:00'),
           (urban_id, 3, '10:00', '13:00'), (urban_id, 3, '14:00', '20:00'),
           (urban_id, 4, '10:00', '13:00'), (urban_id, 4, '14:00', '20:00'),
           (urban_id, 5, '10:00', '13:00'), (urban_id, 5, '14:00', '20:00'),
           (urban_id, 6, '10:00', '16:00');

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Ben', 'Admin', 'ADMIN', urban_id, 'ben.admin@urbancuts.example.com', pw_hash)
    RETURNING id INTO urban_admin_id;

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Leo', 'Barber', 'EMPLOYEE', urban_id, 'leo.barber@urbancuts.example.com', pw_hash)
    RETURNING id INTO urban_emp1_id;
    INSERT INTO staff (user_id, business_id, title, is_active, hired_at) VALUES (urban_emp1_id, urban_id, 'Barber', true, '2020-11-02');

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Sam', 'Barber', 'EMPLOYEE', urban_id, 'sam.barber@urbancuts.example.com', pw_hash)
    RETURNING id INTO urban_emp2_id;
    INSERT INTO staff (user_id, business_id, title, is_active, hired_at) VALUES (urban_emp2_id, urban_id, 'Barber', true, '2023-02-20');

    -- =========================================================================
    -- Business 3: Serenity Day Spa (PENDING)
    -- =========================================================================
    INSERT INTO businesses (name, description, image, status)
    VALUES ('Serenity Day Spa',
            'Massage, body treatments and facials in a quiet setting.',
            'https://picsum.photos/seed/serenity-day-spa/800/500',
            'PENDING')
    RETURNING id INTO serenity_id;

    INSERT INTO addresses (street, city, country, postal_code, business_id)
    VALUES ('8 Lindenweg', 'Munich', 'Germany', '80331', serenity_id);

    INSERT INTO contacts (type, value, business_id)
    VALUES ('phone', '+49 89 1234503', serenity_id),
           ('email', 'hello@serenityspa.example.com', serenity_id);

    INSERT INTO services (business_id, name, description, duration_minutes, price, is_active)
    VALUES (serenity_id, 'Deep Tissue Massage', 'Firm pressure for muscle tension.', 60, 80.00, true);

    INSERT INTO services (business_id, name, description, duration_minutes, price, is_active)
    VALUES (serenity_id, 'Hot Stone Therapy', 'Heated basalt stones with massage.', 90, 110.00, true);

    INSERT INTO services (business_id, name, description, duration_minutes, price, is_active)
    VALUES (serenity_id, 'Facial Renewal', 'Deep cleanse, exfoliation and mask. Currently paused.', 50, 65.00, false);

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Clara', 'Admin', 'ADMIN', serenity_id, 'clara.admin@serenityspa.example.com', pw_hash)
    RETURNING id INTO serenity_admin_id;

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Ida', 'Therapist', 'EMPLOYEE', serenity_id, 'ida.therapist@serenityspa.example.com', pw_hash)
    RETURNING id INTO serenity_emp1_id;
    INSERT INTO staff (user_id, business_id, title, is_active, hired_at) VALUES (serenity_emp1_id, serenity_id, 'Massage Therapist', true, '2022-04-18');

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Tom', 'Therapist', 'EMPLOYEE', serenity_id, 'tom.therapist@serenityspa.example.com', pw_hash)
    RETURNING id INTO serenity_emp2_id;
    INSERT INTO staff (user_id, business_id, title, is_active, hired_at) VALUES (serenity_emp2_id, serenity_id, 'Esthetician', false, '2019-09-09');

    -- =========================================================================
    -- Customers - a salon's own records (DB-03), no login.
    -- =========================================================================
    INSERT INTO customers (business_id, first_name, last_name, email, phone, notes, marketing_consent) VALUES
        (glow_id, 'Olivia', 'Customer', 'olivia@example.com', '+49 30 9990001', 'Prefers morning appointments.', true),
        (glow_id, 'Liam', 'Customer', NULL, '+49 30 9990002', NULL, false),
        (urban_id, 'Emma', 'Customer', 'emma@example.com', '+49 30 9990003', NULL, true),
        (urban_id, 'Ava', 'Customer', NULL, '+49 30 9990004', 'Sensitive scalp.', false),
        (serenity_id, 'Grace', 'Customer', 'grace@example.com', '+49 30 9990005', NULL, false),
        (serenity_id, 'Mason', 'Customer', NULL, '+49 30 9990006', NULL, false);

    -- =========================================================================
    -- Staff working weeks (DB-14): everyone performs every service of their
    -- salon and works its opening hours. Mia is off over Christmas.
    -- =========================================================================
    INSERT INTO staff_services (business_id, staff_id, service_id)
    SELECT st.business_id, st.id, sv.id FROM staff st JOIN services sv ON sv.business_id = st.business_id;

    INSERT INTO staff_schedules (staff_id, day_of_week, starts_at, ends_at)
    SELECT st.id, h.day_of_week, h.opens_at, h.closes_at FROM staff st JOIN business_hours h ON h.business_id = st.business_id;

    INSERT INTO staff_time_off (staff_id, starts_at, ends_at, note)
    SELECT id, '2026-12-24 00:00 Europe/Berlin', '2026-12-27 00:00 Europe/Berlin', 'Christmas'
    FROM staff WHERE user_id = glow_emp1_id;

END $$;

COMMIT;
