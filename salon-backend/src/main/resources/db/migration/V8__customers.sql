/*
  =======================
   DB-03: a salon's customers are their own records, not users rows
  =======================
 */
-- A customer belongs to one salon and needn't have an account. user_id links the record to a
-- login once the customer app exists; it is only set here for customers migrated from users.
CREATE TABLE customers (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    user_id BIGINT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    notes TEXT,
    marketing_consent BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP,

    -- A salon's customer list goes with the salon.
    CONSTRAINT fk_customers_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    -- Deleting the account keeps the salon's record of the customer.
    CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX customers_business_id_idx ON customers (business_id);
CREATE INDEX customers_user_id_idx ON customers (user_id);
-- One account is one customer per salon.
CREATE UNIQUE INDEX customers_business_user_unique_idx ON customers (business_id, user_id)
    WHERE user_id IS NOT NULL;

CREATE TRIGGER customers_set_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Carry over every business_customers link. That table had no PK or FKs, so skip links to rows
-- that no longer exist and collapse duplicates, keeping the earliest join date.
INSERT INTO customers (business_id, user_id, first_name, last_name, email, phone, created_at)
SELECT DISTINCT ON (bc.business_id, bc.user_id)
       bc.business_id, u.id, u.first_name, u.last_name, u.email,
       (SELECT c.value FROM contacts c WHERE c.user_id = u.id AND c.type = 'phone' ORDER BY c.id LIMIT 1),
       bc.joined_at
FROM business_customers bc
JOIN users u ON u.id = bc.user_id
JOIN businesses b ON b.id = bc.business_id
ORDER BY bc.business_id, bc.user_id, bc.joined_at;

DROP TABLE business_customers;
