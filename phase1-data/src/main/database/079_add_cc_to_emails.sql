UPDATE schema_version SET version = 79;

ALTER TABLE emails ADD COLUMN cc TEXT;
