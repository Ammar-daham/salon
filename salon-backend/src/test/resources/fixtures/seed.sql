-- Deterministic fixture loaded before every integration test. Ids are fixed so tests can
-- reference them directly; keep them in sync with IntegrationTest.Fixture.
-- Every login-capable user's password is "Password123!".

INSERT INTO businesses (id, name, description, image, status) VALUES
    (1, 'Glow Beauty Studio', 'Colour and precision cuts.', 'https://example.com/glow.png', 'APPROVED'),
    (2, 'Urban Cuts Barbershop', 'Fades and beard work.', 'https://example.com/urban.png', 'APPROVED'),
    (3, 'Serenity Day Spa', 'Massage and facials.', 'https://example.com/serenity.png', 'PENDING');

INSERT INTO users (id, first_name, last_name, role, business_id, email, password_hash) VALUES
    (1, 'Sam', 'Root', 'SUPER_ADMIN', NULL, 'superadmin@salon.test', '$2b$10$zxJ0N4amu7cL1Zx565YfJ.oWgQmgVcfy51f3tzVc0dimXWluIOxhm'),
    (2, 'Anna', 'Admin', 'ADMIN', 1, 'anna.admin@glow.test', '$2b$10$zxJ0N4amu7cL1Zx565YfJ.oWgQmgVcfy51f3tzVc0dimXWluIOxhm'),
    (3, 'Ben', 'Admin', 'ADMIN', 2, 'ben.admin@urban.test', '$2b$10$zxJ0N4amu7cL1Zx565YfJ.oWgQmgVcfy51f3tzVc0dimXWluIOxhm'),
    (4, 'Mia', 'Stylist', 'EMPLOYEE', 1, 'mia.stylist@glow.test', '$2b$10$zxJ0N4amu7cL1Zx565YfJ.oWgQmgVcfy51f3tzVc0dimXWluIOxhm'),
    (5, 'Olivia', 'Customer', 'CUSTOMER', NULL, NULL, NULL),
    (6, 'Leo', 'Barber', 'EMPLOYEE', 2, 'leo.barber@urban.test', '$2b$10$zxJ0N4amu7cL1Zx565YfJ.oWgQmgVcfy51f3tzVc0dimXWluIOxhm');

INSERT INTO addresses (id, street, city, country, postal_code, business_id, user_id) VALUES
    (1, '12 Rosenthaler Str.', 'Berlin', 'Germany', '10119', 1, NULL),
    (2, '3 Private Lane', 'Berlin', 'Germany', '10115', NULL, 4);

INSERT INTO contacts (id, type, value, business_id, user_id) VALUES
    (1, 'phone', '+49 30 1234501', 1, NULL),
    (2, 'email', 'hello@urban.test', 2, NULL),
    (3, 'phone', '+49 30 9990004', NULL, 4);

INSERT INTO services (id, business_id, name, description, duration_minutes, price, is_active) VALUES
    (1, 1, 'Signature Haircut', 'Wash, cut and style.', 45, 45.00, true),
    (2, 1, 'Classic Manicure', 'Shape and polish.', 30, 25.00, true),
    (3, 2, 'Classic Fade', 'Skin fade.', 30, 28.00, true);

INSERT INTO staff (id, user_id, business_id, title, is_active, hired_at) VALUES
    (1, 4, 1, 'Senior Stylist', true, '2021-03-15'),
    (2, 6, 2, 'Barber', true, '2020-11-02');

INSERT INTO customers (id, business_id, first_name, last_name, email, phone, notes, marketing_consent) VALUES
    (1, 1, 'Olivia', 'Client', 'olivia@example.test', '+49 30 7770001', 'Prefers mornings.', true),
    (2, 2, 'Noah', 'Client', NULL, '+49 30 7770002', NULL, false);

-- Glow: Tuesday to Friday 09:00-18:00, Saturday 10:00-14:00. Urban has no hours yet.
INSERT INTO business_hours (business_id, day_of_week, opens_at, closes_at) VALUES
    (1, 2, '09:00', '18:00'), (1, 3, '09:00', '18:00'), (1, 4, '09:00', '18:00'), (1, 5, '09:00', '18:00'),
    (1, 6, '10:00', '14:00');

-- Mia (Glow staff 1) does haircuts.
INSERT INTO staff_services (business_id, staff_id, service_id) VALUES (1, 1, 1);

SELECT setval(pg_get_serial_sequence('businesses', 'id'), (SELECT max(id) FROM businesses));
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('addresses', 'id'), (SELECT max(id) FROM addresses));
SELECT setval(pg_get_serial_sequence('contacts', 'id'), (SELECT max(id) FROM contacts));
SELECT setval(pg_get_serial_sequence('services', 'id'), (SELECT max(id) FROM services));
SELECT setval(pg_get_serial_sequence('staff', 'id'), (SELECT max(id) FROM staff));
SELECT setval(pg_get_serial_sequence('customers', 'id'), (SELECT max(id) FROM customers));
