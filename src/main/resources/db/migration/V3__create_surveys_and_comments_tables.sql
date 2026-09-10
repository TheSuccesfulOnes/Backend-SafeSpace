CREATE TABLE surveys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    question VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    survey_type VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    allow_comments BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_survey_creator FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE survey_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    answer_text VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_answer_survey FOREIGN KEY (survey_id) REFERENCES surveys(id),
    CONSTRAINT fk_answer_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_answer_survey_user UNIQUE (survey_id, user_id)
);

CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_survey FOREIGN KEY (survey_id) REFERENCES surveys(id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comments(id)
);

CREATE TABLE comment_likes (
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, user_id),
    CONSTRAINT fk_like_comment FOREIGN KEY (comment_id) REFERENCES comments(id),
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_surveys_status ON surveys(status);
CREATE INDEX idx_comments_survey ON comments(survey_id);
