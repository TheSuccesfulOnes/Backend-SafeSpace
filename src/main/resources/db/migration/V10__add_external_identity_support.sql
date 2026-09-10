ALTER TABLE users
    MODIFY COLUMN email VARCHAR(255) NULL,
    MODIFY COLUMN password_hash VARCHAR(255) NULL;

CREATE TABLE external_identities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_external_identity_provider_subject
        UNIQUE (provider, provider_subject),
    CONSTRAINT uk_external_identity_provider_user
        UNIQUE (provider, user_id),
    CONSTRAINT fk_external_identity_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_external_identity_user ON external_identities(user_id);
