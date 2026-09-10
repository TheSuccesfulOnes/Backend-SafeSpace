CREATE TABLE weekly_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE activity_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    label VARCHAR(160) NOT NULL,
    CONSTRAINT fk_option_activity FOREIGN KEY (activity_id) REFERENCES weekly_activities(id)
);

CREATE TABLE activity_votes (
    activity_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (activity_id, user_id),
    CONSTRAINT fk_vote_activity FOREIGN KEY (activity_id) REFERENCES weekly_activities(id),
    CONSTRAINT fk_vote_option FOREIGN KEY (option_id) REFERENCES activity_options(id),
    CONSTRAINT fk_vote_user FOREIGN KEY (user_id) REFERENCES users(id)
);
