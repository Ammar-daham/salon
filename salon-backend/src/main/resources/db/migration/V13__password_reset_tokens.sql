/*
  =======================
   FE-13: reset a forgotten password by email
  =======================
 */
-- One row per reset link sent. Only a SHA-256 of the link's token is kept, so the table alone can't be used to
-- reset anyone's password. A link works once and until it expires, and asking for a new one retires the earlier
-- ones. Rows go with their user.
CREATE TABLE password_reset_tokens (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    -- When the link was used, or replaced by a newer one.
    used_at TIMESTAMPTZ,

    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT password_reset_tokens_token_hash_key UNIQUE (token_hash),
    CONSTRAINT password_reset_tokens_expiry_check CHECK (created_at < expires_at)
);
CREATE INDEX password_reset_tokens_user_id_idx ON password_reset_tokens (user_id);
