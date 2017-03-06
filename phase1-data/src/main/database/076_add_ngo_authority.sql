UPDATE schema_version SET version = 76;

ALTER TABLE v2_itacs ADD COLUMN ngo_authority TEXT;
