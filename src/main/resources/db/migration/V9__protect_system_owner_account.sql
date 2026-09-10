ALTER TABLE users
    ADD COLUMN system_owner BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_users_system_owner ON users(system_owner);
