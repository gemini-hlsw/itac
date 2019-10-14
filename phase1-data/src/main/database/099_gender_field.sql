UPDATE schema_version SET version = 99;

ALTER TABLE v2_investigators ADD COLUMN investigator_gender VARCHAR(20) DEFAULT 'NONE_SELECTED';
