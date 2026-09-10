CREATE TABLE user_preferences (
    user_id BIGINT PRIMARY KEY,
    language VARCHAR(10) NOT NULL DEFAULT 'es',
    theme VARCHAR(20) NOT NULL DEFAULT 'LIGHT',
    CONSTRAINT fk_preferences_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE mood_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    mood VARCHAR(20) NOT NULL,
    mood_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mood_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_mood_user_date UNIQUE (user_id, mood_date)
);

CREATE INDEX idx_mood_date ON mood_entries(mood_date);
