UPDATE schema_version SET version = 68;

ALTER TABLE rollover_observations ADD COLUMN created_timestamp TIMESTAMP NOT NULL DEFAULT Now();
