/*
  =======================
   BE-17 / DB-01: users.business_id references businesses
  =======================
 */
-- Staff of a salon that was deleted before this FK existed point at a business that is gone.
-- Unlink them so the constraint can be added; they were already dangling.
UPDATE users u
SET business_id = NULL
WHERE business_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM businesses b WHERE b.id = u.business_id);

-- RESTRICT: a salon can't be deleted while it still has staff. Remove or move them first.
ALTER TABLE users
ADD CONSTRAINT fk_users_business
FOREIGN KEY (business_id)
REFERENCES businesses(id)
ON DELETE RESTRICT;

/*
  =======================
   BE-12 / DB-11: updated_at is set by the database on every UPDATE
  =======================
 */
CREATE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER businesses_set_updated_at BEFORE UPDATE ON businesses
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER users_set_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER staff_set_updated_at BEFORE UPDATE ON staff
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER addresses_set_updated_at BEFORE UPDATE ON addresses
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER contacts_set_updated_at BEFORE UPDATE ON contacts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER services_set_updated_at BEFORE UPDATE ON services
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
