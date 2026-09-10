ALTER TABLE payment_records
    CHANGE COLUMN beneficiary_username beneficiary_name VARCHAR(100) NOT NULL;
