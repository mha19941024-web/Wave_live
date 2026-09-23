-- Wave Live production migration 0003
-- Adds the credential table required by
-- /api/auth/register and /api/auth/login.

CREATE TABLE IF NOT EXISTS account_credentials (
user_id TEXT PRIMARY KEY,
password_hash TEXT NOT NULL,
created_at TEXT NOT NULL,
updated_at TEXT NOT NULL,

FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE

);

CREATE INDEX IF NOT EXISTS idx_account_credentials_updated
ON account_credentials(updated_at);
