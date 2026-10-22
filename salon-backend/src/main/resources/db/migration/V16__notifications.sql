/*
  =======================
   In-app notifications
  =======================
 */
-- One row per person told, so each reads and clears their own. type says what happened, and business_id which
-- salon it's about; the API puts the salon's name with it as the salon is called now. Rows go with their user,
-- and with the salon.
CREATE TABLE notifications (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    business_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at TIMESTAMPTZ,

    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT notifications_type_check CHECK (type IN ('BUSINESS_REGISTERED'))
);
-- A user's notifications newest first, and how many of them are unread.
CREATE INDEX notifications_user_created_idx ON notifications (user_id, created_at DESC, id DESC);
CREATE INDEX notifications_user_unread_idx ON notifications (user_id) WHERE read_at IS NULL;
CREATE INDEX notifications_business_id_idx ON notifications (business_id);
