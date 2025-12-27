CREATE TABLE capital_gains_report
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(255) NOT NULL,
    tax_year   INT          NOT NULL,
    cost       DECIMAL(38, 18) NOT NULL,
    proceeds   DECIMAL(38, 18) NOT NULL,
    gain       DECIMAL(38, 18) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_capital_gains_report_user_year UNIQUE (user_id, tax_year)
);

CREATE INDEX idx_capital_gains_report_user_year ON capital_gains_report (user_id, tax_year);


