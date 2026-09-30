-- SYNTHETIC seed data only (no AEV data; D7 / A-05). Every name is marked (SAMPLE)
-- and every TIN, email and mobile is fictitious.
INSERT INTO family_group (id, name) VALUES
    ('FG-001', 'Santiago family (SAMPLE)'),
    ('FG-002', 'Del Pilar family (SAMPLE)');
SELECT setval('family_group_id_seq', 2, true);

INSERT INTO stockholder (stockholder_code, name, type, tin, nationality, gender, email, mobile,
                         address, corporation_type, family_group_id, active)
VALUES
    ('SH-001', 'Santiago, Maria Clara (SAMPLE)', 'Individual', '123-456-789-000', 'Filipino',
     'Female', 'mc.santiago@example.test', '+63 900 000 0001', 'Cebu City, PH', NULL, 'FG-001', TRUE),
    ('SH-002', 'Rizal Holdings Corp. (SAMPLE)', 'Corporate', '222-333-444-000', 'Filipino',
     'PreferNotToSay', 'corpsec@rizalholdings.example.test', NULL, 'Makati City, PH',
     'Domestic', NULL, TRUE),
    ('SH-003', 'Del Pilar, Marcelo (SAMPLE)', 'Individual', '333-444-555-000', 'Filipino',
     'Male', 'm.delpilar@example.test', '+63 900 000 0003', 'Bulacan, PH', NULL, 'FG-002', TRUE),
    ('SH-004', 'Global Nominee Ltd. (SAMPLE)', 'Corporate', '444-555-666-000', 'Singaporean',
     'PreferNotToSay', 'registry@globalnominee.example.test', NULL, 'Singapore',
     'Foreign', NULL, TRUE),
    -- A deactivated record, so the demo shows the no-delete lifecycle (OI-19) on screen.
    ('SH-005', 'Bonifacio, Andres (SAMPLE)', 'Individual', '555-666-777-000', 'Filipino',
     'Male', 'a.bonifacio@example.test', NULL, 'Manila, PH', NULL, 'FG-002', FALSE);

-- Generated codes continue at SH-006.
SELECT setval('stockholder_code_seq', 5, true);
