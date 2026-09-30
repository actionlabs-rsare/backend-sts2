-- SYNTHETIC seed data only (no AEV data; D7/A-05). Clearly fictitious.
INSERT INTO company (company_code, name, address, tin, sec_registration_code,
                     incorporation_date, president_name, corporate_secretary_name)
VALUES
    ('CO-001', 'Visayan Electric Company, Inc. (SAMPLE)', 'Cebu City, PH', '000-111-222-000', 'SEC-SAMPLE-001',
     DATE '1998-03-15', 'Sample President A', 'Sample CorpSec A'),
    ('CO-002', 'Hedcor, Inc. (SAMPLE)', 'Davao City, PH', '000-333-444-000', 'SEC-SAMPLE-002',
     DATE '2001-07-22', 'Sample President B', 'Sample CorpSec B'),
    ('CO-003', 'Therma Power (SAMPLE)', 'Taguig City, PH', '000-555-666-000', 'SEC-SAMPLE-003',
     DATE '2010-11-05', 'Sample President C', 'Sample CorpSec C');

-- Advance the sequence past the seeded rows so generated codes continue at CO-004.
SELECT setval('company_code_seq', 3, true);
