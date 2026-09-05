/*
  =======================
   Login credentials for staff/admin users
   (CUSTOMER rows may leave these NULL - they don't log in)
  =======================
 */
ALTER TABLE users
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN password_hash VARCHAR(255);

CREATE UNIQUE INDEX users_email_unique_idx ON users (email) WHERE email IS NOT NULL;
