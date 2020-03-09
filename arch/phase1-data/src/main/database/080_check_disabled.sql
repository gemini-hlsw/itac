-- ITAC-624 Proposal checks - make proposals hidden from checks (i.e. OK) show as hidden for all users with all browsers
-- ITAC-556 Proposal check status reporting

UPDATE schema_version SET version = 80;

ALTER TABLE proposals ADD COLUMN checks_bypassed BOOLEAN DEFAULT FALSE;