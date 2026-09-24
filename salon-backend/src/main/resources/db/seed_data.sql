-- =============================================================================
-- Sample data seed script
-- =============================================================================
--
-- Populates a freshly-migrated database with realistic sample data across every
-- role and resource: a platform super admin, three businesses each with an
-- address, contacts, a handful of services, an admin, two employees (plus a
-- matching `staff` row), and a set of customers linked to businesses via
-- `business_customers`.
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
-- CUSTOMER rows are left with a NULL email/password_hash, matching the app's
-- real behaviour - customers don't log in, per V3's migration comment.
-- =============================================================================

-- TRUNCATE business_customers, business_service, staff, services, contacts,
--     addresses, users, businesses RESTART IDENTITY CASCADE;

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

    svc_id           BIGINT;

    cust1_id BIGINT; cust2_id BIGINT; cust3_id BIGINT;
    cust4_id BIGINT; cust5_id BIGINT; cust6_id BIGINT;
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

    INSERT INTO services (name, description, duration_minutes, price, is_active)
    VALUES ('Signature Haircut', 'Wash, cut and style.', 45, 45.00, true) RETURNING id INTO svc_id;
    INSERT INTO business_service (business_id, service_id) VALUES (glow_id, svc_id);

    INSERT INTO services (name, description, duration_minutes, price, is_active)
    VALUES ('Balayage Colour', 'Hand-painted colour with gloss finish.', 120, 150.00, true) RETURNING id INTO svc_id;
    INSERT INTO business_service (business_id, service_id) VALUES (glow_id, svc_id);

    INSERT INTO services (name, description, duration_minutes, price, is_active)
    VALUES ('Classic Manicure', 'Shape, cuticle care and polish.', 30, 25.00, true) RETURNING id INTO svc_id;
    INSERT INTO business_service (business_id, service_id) VALUES (glow_id, svc_id);

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Anna', 'Admin', 'ADMIN', glow_id, 'anna.admin@glowbeauty.example.com', pw_hash)
    RETURNING id INTO glow_admin_id;

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Mia', 'Stylist', 'EMPLOYEE', glow_id, 'mia.stylist@glowbeauty.example.com', pw_hash)
    RETURNING id INTO glow_emp1_id;
    INSERT INTO staff (user_id, business_id, title, is_active) VALUES (glow_emp1_id, glow_id, 'Senior Stylist', true);

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Noah', 'Colorist', 'EMPLOYEE', glow_id, 'noah.colorist@glowbeauty.example.com', pw_hash)
    RETURNING id INTO glow_emp2_id;
    INSERT INTO staff (user_id, business_id, title, is_active) VALUES (glow_emp2_id, glow_id, 'Colorist', true);

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

    INSERT INTO services (name, description, duration_minutes, price, is_active)
    VALUES ('Classic Fade', 'Skin fade with a straight-razor finish.', 30, 28.00, true) RETURNING id INTO svc_id;
    INSERT INTO business_service (business_id, service_id) VALUES (urban_id, svc_id);

    INSERT INTO services (name, description, duration_minutes, price, is_active)
    VALUES ('Beard Trim', 'Shape and line-up.', 20, 15.00, true) RETURNING id INTO svc_id;
    INSERT INTO business_service (business_id, service_id) VALUES (urban_id, svc_id);

    INSERT INTO services (name, description, duration_minutes, price, is_active)
    VALUES ('Hot Towel Shave', 'Traditional straight-razor shave.', 40, 35.00, true) RETURNING id INTO svc_id;
    INSERT INTO business_service (business_id, service_id) VALUES (urban_id, svc_id);

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Ben', 'Admin', 'ADMIN', urban_id, 'ben.admin@urbancuts.example.com', pw_hash)
    RETURNING id INTO urban_admin_id;

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Leo', 'Barber', 'EMPLOYEE', urban_id, 'leo.barber@urbancuts.example.com', pw_hash)
    RETURNING id INTO urban_emp1_id;
    INSERT INTO staff (user_id, business_id, title, is_active) VALUES (urban_emp1_id, urban_id, 'Barber', true);

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Sam', 'Barber', 'EMPLOYEE', urban_id, 'sam.barber@urbancuts.example.com', pw_hash)
    RETURNING id INTO urban_emp2_id;
    INSERT INTO staff (user_id, business_id, title, is_active) VALUES (urban_emp2_id, urban_id, 'Barber', true);

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

    INSERT INTO services (name, description, duration_minutes, price, is_active)
    VALUES ('Deep Tissue Massage', 'Firm pressure for muscle tension.', 60, 80.00, true) RETURNING id INTO svc_id;
    INSERT INTO business_service (business_id, service_id) VALUES (serenity_id, svc_id);

    INSERT INTO services (name, description, duration_minutes, price, is_active)
    VALUES ('Hot Stone Therapy', 'Heated basalt stones with massage.', 90, 110.00, true) RETURNING id INTO svc_id;
    INSERT INTO business_service (business_id, service_id) VALUES (serenity_id, svc_id);

    INSERT INTO services (name, description, duration_minutes, price, is_active)
    VALUES ('Facial Renewal', 'Deep cleanse, exfoliation and mask. Currently paused.', 50, 65.00, false) RETURNING id INTO svc_id;
    INSERT INTO business_service (business_id, service_id) VALUES (serenity_id, svc_id);

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Clara', 'Admin', 'ADMIN', serenity_id, 'clara.admin@serenityspa.example.com', pw_hash)
    RETURNING id INTO serenity_admin_id;

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Ida', 'Therapist', 'EMPLOYEE', serenity_id, 'ida.therapist@serenityspa.example.com', pw_hash)
    RETURNING id INTO serenity_emp1_id;
    INSERT INTO staff (user_id, business_id, title, is_active) VALUES (serenity_emp1_id, serenity_id, 'Massage Therapist', true);

    INSERT INTO users (first_name, last_name, role, business_id, email, password_hash)
    VALUES ('Tom', 'Therapist', 'EMPLOYEE', serenity_id, 'tom.therapist@serenityspa.example.com', pw_hash)
    RETURNING id INTO serenity_emp2_id;
    INSERT INTO staff (user_id, business_id, title, is_active) VALUES (serenity_emp2_id, serenity_id, 'Esthetician', false);

    -- =========================================================================
    -- Customers - no login, no business_id column, linked to businesses only
    -- through business_customers. Each gets one phone contact.
    -- =========================================================================
    INSERT INTO users (first_name, last_name, role) VALUES ('Olivia', 'Customer', 'CUSTOMER') RETURNING id INTO cust1_id;
    INSERT INTO users (first_name, last_name, role) VALUES ('Liam', 'Customer', 'CUSTOMER') RETURNING id INTO cust2_id;
    INSERT INTO users (first_name, last_name, role) VALUES ('Emma', 'Customer', 'CUSTOMER') RETURNING id INTO cust3_id;
    INSERT INTO users (first_name, last_name, role) VALUES ('Ava', 'Customer', 'CUSTOMER') RETURNING id INTO cust4_id;
    INSERT INTO users (first_name, last_name, role) VALUES ('Grace', 'Customer', 'CUSTOMER') RETURNING id INTO cust5_id;
    INSERT INTO users (first_name, last_name, role) VALUES ('Mason', 'Customer', 'CUSTOMER') RETURNING id INTO cust6_id;

    INSERT INTO contacts (type, value, user_id) VALUES
        ('phone', '+49 30 9990001', cust1_id),
        ('phone', '+49 30 9990002', cust2_id),
        ('phone', '+49 30 9990003', cust3_id),
        ('phone', '+49 30 9990004', cust4_id),
        ('phone', '+49 30 9990005', cust5_id),
        ('phone', '+49 30 9990006', cust6_id);

    INSERT INTO business_customers (business_id, user_id) VALUES
        (glow_id, cust1_id),
        (glow_id, cust2_id),
        (urban_id, cust3_id),
        (urban_id, cust4_id),
        (serenity_id, cust5_id),
        (serenity_id, cust6_id);

END $$;

COMMIT;
