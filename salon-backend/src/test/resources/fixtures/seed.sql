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

INSERT INTO services (id, name, description, duration_minutes, price, is_active) VALUES
    (1, 'Signature Haircut', 'Wash, cut and style.', 45, 45.00, true),
    (2, 'Classic Manicure', 'Shape and polish.', 30, 25.00, true),
    (3, 'Classic Fade', 'Skin fade.', 30, 28.00, true);

INSERT INTO business_service (business_id, service_id) VALUES (1, 1), (1, 2), (2, 3);

SELECT setval(pg_get_serial_sequence('businesses', 'id'), (SELECT max(id) FROM businesses));
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('addresses', 'id'), (SELECT max(id) FROM addresses));
SELECT setval(pg_get_serial_sequence('contacts', 'id'), (SELECT max(id) FROM contacts));
SELECT setval(pg_get_serial_sequence('services', 'id'), (SELECT max(id) FROM services));
