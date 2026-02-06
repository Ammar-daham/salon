/*
  =======================
   Linking tables / Adding constraint
  =======================
 */
ALTER TABLE contacts
ADD CONSTRAINT fk_contacts_business
FOREIGN KEY (business_id)
REFERENCES businesses(id)
ON DELETE SET NULL;

ALTER TABLE contacts
ADD CONSTRAINT fk_contacts_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE SET NULL;

ALTER TABLE addresses
ADD CONSTRAINT fk_addresses_business
FOREIGN KEY (business_id)
REFERENCES businesses(id)
ON DELETE SET NULL;

ALTER TABLE addresses
ADD CONSTRAINT fk_addresses_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE SET NULL;

ALTER TABLE staff
ADD CONSTRAINT uq_staff_user
UNIQUE (user_id);

ALTER TABLE staff
ADD CONSTRAINT fk_staff_user
FOREIGN KEY (user_id)
REFERENCES users(id)
ON DELETE CASCADE;

ALTER TABLE staff
ADD CONSTRAINT fk_staff_business
FOREIGN KEY (business_id)
REFERENCES businesses(id)
ON DELETE CASCADE;

ALTER TABLE users
ADD CONSTRAINT users_role_check
CHECK (role IN ('CUSTOMER', 'EMPLOYEE', 'ADMIN', 'SUPER_ADMIN'));

ALTER TABLE business_service
ADD CONSTRAINT fk_business
FOREIGN KEY (business_id)
REFERENCES businesses (id)
ON DELETE CASCADE;

ALTER TABLE business_service
ADD CONSTRAINT fk_service
FOREIGN KEY (service_id)
REFERENCES services(id)
ON DELETE CASCADE;



