-- Unit S1 · Master Data — stockholders + family groups (MDM-002, MDM-002-FG).
-- Forward-only, backward-compatible (practices.md). Never edit a merged migration.
--
-- Version range convention for Wave 1 (proposed to the integrator as CR-2 so the three teams
-- do not collide in the single Flyway location): Wave 0 = V1-V9, S1 = V10-V19,
-- S2 = V20-V29, S3 = V30-V39.

-- Stockholder Code sequence (MDM-002). Deliberately separate from company_code_seq:
-- a stockholder is a separate entity from a company, and a company may be a stockholder elsewhere.
CREATE SEQUENCE IF NOT EXISTS stockholder_code_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS family_group_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE family_group (
    id   VARCHAR(20)  PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE stockholder (
    stockholder_code VARCHAR(20)  PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    type             VARCHAR(20)  NOT NULL,
    -- Personal data (PH Data Privacy Act): encrypted at rest by Aurora, masked in logs,
    -- role-restricted read (SL-005). Required by BRD MDM-002.
    tin              VARCHAR(40)  NOT NULL,
    nationality      VARCHAR(60)  NOT NULL,
    gender           VARCHAR(20)  NOT NULL,
    email            VARCHAR(200) NOT NULL,
    mobile           VARCHAR(40),
    address          VARCHAR(300),
    corporation_type VARCHAR(20),
    family_group_id  VARCHAR(20),
    -- OI-19 / Gate 3 Q4: records are never deleted; this flag carries the lifecycle.
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_stockholder_type CHECK (type IN ('Individual', 'Corporate')),
    CONSTRAINT ck_stockholder_gender CHECK (gender IN ('Female', 'Male', 'PreferNotToSay')),
    CONSTRAINT ck_stockholder_corp_type CHECK (corporation_type IS NULL
                                               OR corporation_type IN ('Domestic', 'Foreign')),
    -- Corporate details only on corporate stockholders (mirrors Stockholder.normalise()).
    CONSTRAINT ck_stockholder_corp_only CHECK (corporation_type IS NULL OR type = 'Corporate'),
    CONSTRAINT fk_stockholder_family_group FOREIGN KEY (family_group_id)
        REFERENCES family_group (id)
);

CREATE INDEX idx_stockholder_name ON stockholder (lower(name));
CREATE INDEX idx_stockholder_family_group ON stockholder (family_group_id);
