CREATE TABLE payment_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    beneficiary_username VARCHAR(50) NOT NULL,
    recorded_by_user_id BIGINT NULL,
    recorded_by_username VARCHAR(50) NOT NULL,
    plan VARCHAR(20) NOT NULL,
    voucher_filename VARCHAR(255) NOT NULL,
    voucher_content_type VARCHAR(100) NOT NULL,
    voucher_size BIGINT NOT NULL,
    voucher_data LONGBLOB NOT NULL,
    next_payment_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_beneficiary
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_payment_recorded_by
        FOREIGN KEY (recorded_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_payment_user_id (user_id),
    INDEX idx_payment_next_payment_date (next_payment_date)
);
