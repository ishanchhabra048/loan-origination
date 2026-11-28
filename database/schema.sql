-- Mini Loan Origination Application | Java 17 + JDBC | MySQL 8.0.16+
-- DEVELOPMENT RESET: Running this script drops existing application tables and data.
CREATE DATABASE IF NOT EXISTS loan_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE loan_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ng_core_banking;
DROP TABLE IF EXISTS utility_run;
DROP TABLE IF EXISTS system_alerts;
DROP TABLE IF EXISTS application_history;
DROP TABLE IF EXISTS disbursement;
DROP TABLE IF EXISTS loan_application;
DROP TABLE IF EXISTS product_config;
DROP TABLE IF EXISTS customer;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Customer
CREATE TABLE customer (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    mobile CHAR(10) NOT NULL,
    address VARCHAR(255),
    kyc_details VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_mobile UNIQUE (mobile),
    CONSTRAINT chk_customer_mobile CHECK (mobile REGEXP '^[0-9]{10}$'),
    CONSTRAINT chk_customer_name CHECK (CHAR_LENGTH(TRIM(name)) > 0)
) ENGINE=InnoDB;
CREATE INDEX idx_customer_name ON customer(name);

-- 2. Configurable loan products
CREATE TABLE product_config (
    product_code VARCHAR(20) PRIMARY KEY,
    min_amount DECIMAL(15,2) NOT NULL,
    max_amount DECIMAL(15,2) NOT NULL,
    min_tenure INT NOT NULL,
    max_tenure INT NOT NULL,
    min_income DECIMAL(15,2) NOT NULL,
    annual_rate DECIMAL(5,2) NOT NULL,
    co_borrower_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_product_amount CHECK (min_amount > 0 AND max_amount >= min_amount),
    CONSTRAINT chk_product_tenure CHECK (min_tenure > 0 AND max_tenure >= min_tenure),
    CONSTRAINT chk_product_income CHECK (min_income > 0),
    CONSTRAINT chk_product_rate CHECK (annual_rate > 0 AND annual_rate <= 100)
) ENGINE=InnoDB;

-- 3. Loan applications and assessment snapshot
CREATE TABLE loan_application (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_number VARCHAR(30) NOT NULL,
    customer_id BIGINT NOT NULL,
    product VARCHAR(20) NOT NULL,
    branch_code VARCHAR(10) NOT NULL,
    branch_name VARCHAR(60) NOT NULL,
    requested_amount DECIMAL(15,2) NOT NULL,
    tenure_months INT NOT NULL,
    monthly_income DECIMAL(15,2) NOT NULL,
    existing_monthly_obligations DECIMAL(15,2) NOT NULL DEFAULT 0,
    has_co_borrower BOOLEAN NOT NULL DEFAULT FALSE,
    annual_interest_rate DECIMAL(5,2) NOT NULL,
    proposed_emi DECIMAL(15,2),
    foir_percentage DECIMAL(7,2),
    eligible BOOLEAN,
    eligibility_reasons TEXT,
    status VARCHAR(25) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL,
    submitted_at DATETIME,
    decided_at DATETIME,
    reviewer_code VARCHAR(30),
    decision_reason VARCHAR(500),
    form_path VARCHAR(255),
    -- PDF definition: active = DRAFT, UNDER_REVIEW, APPROVED.
    -- Unique nullable generated key allows multiple closed applications.
    active_key VARCHAR(60) GENERATED ALWAYS AS (
        CASE WHEN status IN ('DRAFT','UNDER_REVIEW','APPROVED')
             THEN CONCAT(customer_id, '-', product)
             ELSE NULL END
    ) STORED,
    CONSTRAINT uq_application_number UNIQUE (application_number),
    CONSTRAINT uq_one_active_per_product UNIQUE (active_key),
    CONSTRAINT fk_application_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id),
    CONSTRAINT fk_application_product FOREIGN KEY (product) REFERENCES product_config(product_code),
    CONSTRAINT chk_application_status CHECK (status IN ('DRAFT','UNDER_REVIEW','APPROVED','REJECTED','DISBURSED','CORE_BANKING_PUSH','EXIT')),
    CONSTRAINT chk_application_amount CHECK (requested_amount > 0),
    CONSTRAINT chk_application_tenure CHECK (tenure_months > 0),
    CONSTRAINT chk_application_income CHECK (monthly_income > 0),
    CONSTRAINT chk_application_obligations CHECK (existing_monthly_obligations >= 0),
    CONSTRAINT chk_application_rate CHECK (annual_interest_rate > 0 AND annual_interest_rate <= 100),
    CONSTRAINT chk_application_emi CHECK (proposed_emi IS NULL OR proposed_emi >= 0),
    CONSTRAINT chk_application_foir CHECK (foir_percentage IS NULL OR foir_percentage >= 0)
) ENGINE=InnoDB;
CREATE INDEX idx_application_status_submitted ON loan_application(status, submitted_at);
CREATE INDEX idx_application_customer ON loan_application(customer_id);
CREATE INDEX idx_application_branch ON loan_application(branch_code);
CREATE INDEX idx_application_product_status ON loan_application(product, status);

-- 4. One disbursement per approved application (status enforced in Java)
CREATE TABLE disbursement (
    disbursement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    reference VARCHAR(40) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    disbursed_at DATETIME NOT NULL,
    CONSTRAINT uq_disbursement_application UNIQUE (application_id),
    CONSTRAINT uq_disbursement_reference UNIQUE (reference),
    CONSTRAINT fk_disbursement_application FOREIGN KEY (application_id) REFERENCES loan_application(application_id),
    CONSTRAINT chk_disbursement_amount CHECK (amount > 0)
) ENGINE=InnoDB;
CREATE INDEX idx_disbursement_date ON disbursement(disbursed_at);

-- 5. Application audit history
CREATE TABLE application_history (
    history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    action VARCHAR(40) NOT NULL,
    old_status VARCHAR(25),
    new_status VARCHAR(25),
    performed_by VARCHAR(30) NOT NULL,
    performed_at DATETIME NOT NULL,
    remarks VARCHAR(500),
    CONSTRAINT fk_history_application FOREIGN KEY (application_id) REFERENCES loan_application(application_id)
) ENGINE=InnoDB;
CREATE INDEX idx_history_application_time ON application_history(application_id, performed_at);

-- 6. Persistent overdue alerts (no duplicate application/type)
CREATE TABLE system_alerts (
    alert_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    alert_type VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uq_system_alert UNIQUE (application_id, alert_type),
    CONSTRAINT fk_alert_application FOREIGN KEY (application_id) REFERENCES loan_application(application_id)
) ENGINE=InnoDB;

-- 7. Scheduled utility execution log
CREATE TABLE utility_run (
    run_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    utility_name VARCHAR(40) NOT NULL,
    started_at DATETIME NOT NULL,
    completed_at DATETIME,
    result VARCHAR(20),
    applications_processed INT NOT NULL DEFAULT 0,
    alerts_created INT NOT NULL DEFAULT 0,
    error_message TEXT,
    CONSTRAINT chk_utility_counts CHECK (applications_processed >= 0 AND alerts_created >= 0),
    CONSTRAINT chk_utility_result CHECK (result IS NULL OR result IN ('SUCCESS','FAILED','PARTIAL')),
    CONSTRAINT chk_utility_times CHECK (completed_at IS NULL OR completed_at >= started_at)
) ENGINE=InnoDB;

-- 8. Simulated core banking integration
CREATE TABLE ng_core_banking (
    core_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    dob DATE NOT NULL,
    kyc_details VARCHAR(255),
    approved_amount DECIMAL(15,2) NOT NULL,
    disbursed_amount DECIMAL(15,2) NOT NULL,
    disbursed_date DATE NOT NULL,
    pushed_at DATETIME NOT NULL,
    CONSTRAINT uq_core_application UNIQUE (application_id),
    CONSTRAINT fk_core_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id),
    CONSTRAINT fk_core_application FOREIGN KEY (application_id) REFERENCES loan_application(application_id),
    CONSTRAINT chk_core_amounts CHECK (approved_amount > 0 AND disbursed_amount > 0)
) ENGINE=InnoDB;

-- Product rules specified by Newgen assignment
INSERT INTO product_config
    (product_code, min_amount, max_amount, min_tenure, max_tenure, min_income, annual_rate, co_borrower_allowed)
VALUES
    ('PERSONAL', 50000.00, 500000.00, 12, 60, 20000.00, 12.00, TRUE),
    ('EDUCATION', 100000.00, 1000000.00, 12, 84, 15000.00, 10.00, FALSE);

-- Quick verification
SHOW TABLES;
SELECT * FROM product_config;
