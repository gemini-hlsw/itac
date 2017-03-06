UPDATE schema_version SET version = 69;

ALTER TABLE rollover_observations ADD COLUMN created_timestamp TIMESTAMP NOT NULL DEFAULT Now();

DROP TABLE IF EXISTS shutdowns CASCADE;

CREATE TABLE shutdowns (
    id integer PRIMARY KEY,
    date_range_id integer not null,
    site_id integer not null,
    committee_id integer --Nullable since we want to null it out in delete transaction
);
