-- SYNTHETIC seed data only (no AEV data; D7 / A-05). Figures match mockups/company-shares.html
-- so the demo screen and the mockup agree.
INSERT INTO share_class (id, company_code, stock_type, par_value,
                         authorized_shares, subscribed_shares, issued_shares, treasury_shares)
VALUES
    ('SC-000001', 'CO-001', 'Common', 100.0000, 1000000, 800000, 750000, 0),
    ('SC-000002', 'CO-001', 'Redeemable Preferred', 50.0000, 200000, 120000, 120000, 5000),
    ('SC-000003', 'CO-002', 'Common', 25.0000, 500000, 400000, 400000, 0);

SELECT setval('share_class_id_seq', 3, true);

-- The independent certificate sequences for the seeded classes, both series starting at 1
-- (SL-002, TR-004). Runtime-created classes get theirs from CertificateSequenceStore, which uses
-- the same names (CertificateSequenceNames).
CREATE SEQUENCE IF NOT EXISTS certificate_seq_sc_000001 START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS certificate_replacement_seq_sc_000001 START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS certificate_seq_sc_000002 START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS certificate_replacement_seq_sc_000002 START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS certificate_seq_sc_000003 START WITH 1 INCREMENT BY 1 NO CYCLE;
CREATE SEQUENCE IF NOT EXISTS certificate_replacement_seq_sc_000003 START WITH 1 INCREMENT BY 1 NO CYCLE;
