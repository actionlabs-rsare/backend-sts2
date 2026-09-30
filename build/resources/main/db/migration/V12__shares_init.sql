-- Unit S1 · Master Data — company share classes (MDM-003).
-- Each class owns an independent certificate-number sequence starting at 1, plus a separate
-- replacement sequence (SL-002, TR-004). Those sequences are created by the application when a
-- class is defined (CertificateSequenceStore); the ones for the seeded classes are created below.

CREATE SEQUENCE IF NOT EXISTS share_class_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE share_class (
    id                VARCHAR(20)    PRIMARY KEY,
    company_code      VARCHAR(20)    NOT NULL,
    stock_type        VARCHAR(60)    NOT NULL,
    par_value         NUMERIC(19, 4) NOT NULL,
    authorized_shares BIGINT         NOT NULL DEFAULT 0,
    subscribed_shares BIGINT         NOT NULL DEFAULT 0,
    issued_shares     BIGINT         NOT NULL DEFAULT 0,
    treasury_shares   BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_share_class_company FOREIGN KEY (company_code)
        REFERENCES company (company_code),
    -- One class per stock type per company: the certificate sequence is keyed on the class,
    -- so duplicates would split one series in two.
    CONSTRAINT uq_share_class_company_type UNIQUE (company_code, stock_type),
    CONSTRAINT ck_share_class_par_value CHECK (par_value >= 0),
    -- Counts nest (mirrors ShareClassService.applyCounts).
    CONSTRAINT ck_share_class_counts CHECK (
        subscribed_shares <= authorized_shares
        AND issued_shares <= subscribed_shares
        AND treasury_shares <= issued_shares
    )
);

CREATE INDEX idx_share_class_company ON share_class (company_code);

-- NOTE: total_amount is deliberately absent. It is authorized_shares × par_value, derived on read
-- (MDM-003 "automatically computes"), so it cannot drift from its inputs.
